package app.aryan447.mpvium.ui.player

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against Kotlin's silently allowed "read a field before it is initialized" bug.
 *
 * Property initializers and `init` blocks run in declaration order, so an `init` block that
 * reads a property declared *below* it reads `null` (for reference types) at runtime. That is
 * exactly how `PlayerViewModel` crashed on launch:
 *
 * ```
 * init { viewModelScope.launch { combine(controlsShown, seekBarShown, ...) } }  // line 196
 * ...
 * val controlsShown: StateFlow<Boolean> = _controlsShown.asStateFlow()          // line 284
 * ```
 *
 * The compiler does not report this, and no unit test can construct a ViewModel without
 * Android + Koin. This test therefore checks member ordering directly in the sources.
 *
 * Rule: an `init` block may call functions and read constructor parameters freely, but it may
 * not reference a `val`/`var` declared after it (even inside a lambda, because
 * `Dispatchers.Main.immediate` runs those bodies during construction).
 */
class ViewModelInitOrderTest {
  @Test
  fun playerViewModel_neverReadsPropertyBeforeItsInitializer() {
    val source = sourceFile("PlayerViewModel.kt")
    val violations = InitOrderChecker.findViolations(source.readText(), source.name)
    assertEquals(
      "PlayerViewModel init blocks read properties declared below them; " +
        "move the block below the property declarations:\n" +
        violations.joinToString("\n"),
      emptyList<String>(),
      violations,
    )
  }

  @Test
  fun allViewModels_neverReadPropertyBeforeItsInitializer() {
    val violations = mainSourceRoot()
      .walkTopDown()
      .filter { it.isFile && it.name.endsWith("ViewModel.kt") }
      .flatMap { file ->
        InitOrderChecker.findViolations(file.readText(), file.name).asSequence()
      }
      .toList()

    assertEquals(
      "Init blocks must not read ViewModel state declared below them (it is null at runtime):\n" +
        violations.joinToString("\n"),
      emptyList<String>(),
      violations,
    )
  }

  @Test
  fun playerViewModel_leanCachePollingIsWiredAfterItsStateIsDeclared() {
    val text = sourceFile("PlayerViewModel.kt").readText()

    // The regression that crashed 1.3.0-377: the visibility/leanSeekCache combine observed
    // the ViewModel state before `_controlsShown`/`_seekBarShown` existed.
    val initIndex = text.indexOf("combine(controlsShown, seekBarShown, advancedPreferences.leanSeekCache.changes())")
    assertTrue("Lean cache polling wiring must exist in PlayerViewModel", initIndex >= 0)

    assertTrue(
      "`controlsShown` must be declared before the lean cache polling observer",
      text.indexOf("val controlsShown: StateFlow<Boolean>") in 0 until initIndex,
    )
    assertTrue(
      "`seekBarShown` must be declared before the lean cache polling observer",
      text.indexOf("val seekBarShown: StateFlow<Boolean>") in 0 until initIndex,
    )
  }

  @Test
  fun checker_detectsReadBeforeDeclaration() {
    val source =
      """
      class Broken(
        private val host: String,
      ) {
        init {
          viewModelScope.launch {
            controlsShown.collect { value -> println(value) }
          }
        }

        private val _controlsShown = MutableStateFlow(false)
        val controlsShown: StateFlow<Boolean> = _controlsShown.asStateFlow()
      }
      """.trimIndent()

    val violations = InitOrderChecker.findViolations(source, "Broken.kt")
    assertEquals(listOf("Broken.kt:4: init block reads `controlsShown` (declared line 11)"), violations)
  }

  @Test
  fun checker_allowsLocalsParamsAndFunctionsDeclaredLater() {
    val source =
      """
      class Fine(
        private val host: String,
      ) {
        init {
          viewModelScope.launch {
            val local = compute(host)
            local.collect { value -> update(value) }
          }
        }

        fun update(value: String) = Unit

        private fun compute(input: String): Flow<String> = flowOf(input)
      }
      """.trimIndent()

    assertEquals(emptyList<String>(), InitOrderChecker.findViolations(source, "Fine.kt"))
  }

  private fun sourceFile(name: String): File =
    mainSourceRoot().walkTopDown().firstOrNull { it.isFile && it.name == name }
      ?: throw AssertionError("Could not find $name under ${mainSourceRoot()}")

  private fun mainSourceRoot(): File {
    val relative = "src/main/java"
    // Unit tests run with the module directory (`app/`) as the working directory, but Gradle
    // does not guarantee that for every IDE/CI invocation, so walk up to the module root.
    var dir: File? = File("").absoluteFile
    while (dir != null) {
      val candidate = File(dir, relative)
      if (candidate.isDirectory) return candidate
      val nested = File(dir, "app/$relative")
      if (nested.isDirectory) return nested
      dir = dir.parentFile
    }
    throw AssertionError("Could not locate $relative from ${File("").absolutePath}")
  }
}

/**
 * Minimal Kotlin member-order scanner. It intentionally ignores member functions, constructor
 * parameters and locals: only `val`/`var` fields take part in initialization order.
 */
internal object InitOrderChecker {
  private val PROPERTY = Regex(
    "^(?:(?:private|internal|public|protected|override|lateinit|const)\\s+)*(?:val|var)\\s+(\\w+)",
  )
  private val INIT_BLOCK = Regex("^init\\s*\\{")
  private val CLASS_DECL = Regex(
    "^\\s*(?:(?:private|internal|public|open|abstract|sealed|data|annotation|value|enum|inner)\\s+)*" +
      "(?:class|object)\\s+(\\w+)",
  )
  private val DECLARATION_START = Regex(
    "^(?:(?:private|internal|public|protected|open|abstract|sealed|data|annotation|value|enum|inner" +
      "|override|lateinit|const)\\s+)*(?:class|object|interface|fun|val|var|init)\\b",
  )

  fun findViolations(source: String, fileName: String): List<String> {
    val lines = stripCommentsAndStrings(source).split('\n')
    val violations = mutableListOf<String>()
    val handledBodies = mutableListOf<IntRange>()

    for (index in lines.indices) {
      if (!CLASS_DECL.containsMatchIn(lines[index])) continue
      if (handledBodies.any { index in it }) continue

      val bodyOpen = findBodyBrace(lines, index) ?: continue
      val bodyEnd = findMatchingBrace(lines, bodyOpen.first, bodyOpen.second) ?: continue
      handledBodies += bodyOpen.first..bodyEnd.first
      violations += checkClassBody(lines, bodyOpen, bodyEnd, fileName)
    }
    return violations
  }

  private fun checkClassBody(
    lines: List<String>,
    bodyOpen: Pair<Int, Int>,
    bodyEnd: Pair<Int, Int>,
    fileName: String,
  ): List<String> {
    val body = lines
      .subList(bodyOpen.first, bodyEnd.first + 1)
      .mapIndexed { offset, text ->
        var line = text
        if (offset == 0) line = line.substring(bodyOpen.second + 1)
        if (bodyOpen.first + offset == bodyEnd.first) {
          val close = line.lastIndexOf('}')
          if (close >= 0) line = line.substring(0, close)
        }
        line
      }
    val baseLine = bodyOpen.first + 1 // 1-based line number of body[0]

    val properties = mutableMapOf<String, Int>()
    val initBlocks = mutableListOf<Pair<Int, String>>()
    var depth = 0
    var currentInit: Pair<Int, MutableList<String>>? = null

    for ((offset, text) in body.withIndex()) {
      if (depth == 0) {
        val trimmed = text.trim()
        if (INIT_BLOCK.containsMatchIn(trimmed)) {
          currentInit = (baseLine + offset) to mutableListOf()
        } else {
          PROPERTY.find(trimmed)?.let { properties[it.groupValues[1]] = baseLine + offset }
        }
      }
      depth += text.count { it == '{' } - text.count { it == '}' }
      val init = currentInit
      if (init != null) {
        init.second += text
        if (depth == 0) {
          initBlocks += init.first to init.second.joinToString("\n")
          currentInit = null
        }
      }
    }

    val violations = mutableListOf<String>()
    for ((initLine, blockText) in initBlocks) {
      // Names declared inside the block shadow the fields, so they are not reads of `this`.
      val locals = Regex("(?:val|var)\\s+(\\w+)").findAll(blockText)
        .map { it.groupValues[1] }.toMutableSet()
      Regex("([\\w\\s,()]+?)\\s*->").findAll(blockText)
        .forEach { match -> Regex("\\w+").findAll(match.groupValues[1]).forEach { locals += it.value } }

      for ((name, declaredLine) in properties) {
        if (name in locals || declaredLine <= initLine) continue
        if (Regex("\\b$name\\b").containsMatchIn(blockText)) {
          violations += "$fileName:$initLine: init block reads `$name` (declared line $declaredLine)"
        }
      }
    }
    return violations
  }

  /** First `{` after [startLine] that is not inside parentheses (i.e. the class body). */
  private fun findBodyBrace(lines: List<String>, startLine: Int): Pair<Int, Int>? {
    var parens = 0
    for (line in startLine until minOf(startLine + 8, lines.size)) {
      val text = lines[line].trim()
      if (line > startLine && parens == 0 && DECLARATION_START.containsMatchIn(text)) return null
      for ((column, char) in lines[line].withIndex()) {
        when (char) {
          '(' -> parens++
          ')' -> parens--
          '{' -> if (parens == 0) return line to column
          '}' -> if (parens == 0) return null
        }
      }
    }
    return null
  }

  private fun findMatchingBrace(lines: List<String>, line: Int, column: Int): Pair<Int, Int>? {
    var depth = 0
    for (current in line until lines.size) {
      val from = if (current == line) column else 0
      for (index in from until lines[current].length) {
        when (lines[current][index]) {
          '{' -> depth++
          '}' -> {
            depth--
            if (depth == 0) return current to index
          }
        }
      }
    }
    return null
  }

  /** Replaces comments and literals with spaces so line numbers and braces stay intact. */
  private fun stripCommentsAndStrings(source: String): String {
    val out = StringBuilder(source.length)
    var index = 0
    while (index < source.length) {
      when {
        source.startsWith("\"\"\"", index) -> {
          val end = source.indexOf("\"\"\"", index + 3)
          val stop = if (end == -1) source.length else end + 3
          source.substring(index, stop).forEach { out.append(if (it == '\n') '\n' else ' ') }
          index = stop
        }
        source.startsWith("//", index) -> {
          val end = source.indexOf('\n', index)
          val stop = if (end == -1) source.length else end
          repeat(stop - index) { out.append(' ') }
          index = stop
        }
        source.startsWith("/*", index) -> {
          val end = source.indexOf("*/", index + 2)
          val stop = if (end == -1) source.length else end + 2
          source.substring(index, stop).forEach { out.append(if (it == '\n') '\n' else ' ') }
          index = stop
        }
        source[index] == '"' || source[index] == '\'' -> {
          val quote = source[index]
          var cursor = index + 1
          while (cursor < source.length && source[cursor] != quote) {
            cursor += if (source[cursor] == '\\') 2 else 1
          }
          repeat(minOf(cursor + 1, source.length) - index) { out.append(' ') }
          index = minOf(cursor + 1, source.length)
        }
        else -> out.append(source[index++])
      }
    }
    return out.toString()
  }
}
