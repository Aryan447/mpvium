package app.aryan447.mpvium.preferences

/**
 * Utility for parsing generic mpv commands, chained commands, and mpvium Lua snippets
 * into argument lists suitable for [is.xyz.mpv.MPVLib.command].
 */
object CustomCommandParser {

  /**
   * Parses [raw] into one or more mpv command argument lists.
   *
   * Supports:
   * 1. Plain commands: e.g. "cycle pause", "script-binding Script/delete_file", "show-text \"hello world\""
   * 2. mpvium Lua single commands: e.g. mp.command("script-binding Script/delete_file"), mp.command('...'), mp.command([[...]])
   * 3. mpvium Lua multi-arg commands: e.g. mp.commandv("script-binding", "Script/delete_file")
   * 4. Chained commands separated by ';' or newlines outside of quotes: e.g. "cycle pause; show-text \"Paused\""
   */
  fun parse(raw: String): List<List<String>> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return emptyList()

    // 1. mp.commandv(...)
    val commandvMatch = Regex("""^mp\.commandv\s*\(([\s\S]*)\)$""").find(trimmed)
    if (commandvMatch != null) {
      val args = parseCommaSeparatedArguments(commandvMatch.groupValues[1])
      return if (args.isNotEmpty()) listOf(args) else emptyList()
    }

    // 2. mp.command(...)
    var script = trimmed
    val commandMatch = Regex("""^mp\.command\s*\(\s*(?:\[\[|"|')([\s\S]*?)(?:\]\]|"|')\s*\)$""").find(trimmed)
    if (commandMatch != null) {
      script = commandMatch.groupValues[1].trim()
    }

    // 3. Split by ';' or newlines outside of quotes
    val statements = splitStatements(script)
    val result = mutableListOf<List<String>>()
    for (stmt in statements) {
      val tokens = tokenizeArguments(stmt)
      if (tokens.isNotEmpty()) {
        result.add(tokens)
      }
    }
    return result
  }

  private fun parseCommaSeparatedArguments(inner: String): List<String> {
    val list = mutableListOf<String>()
    val current = StringBuilder()
    var inDouble = false
    var inSingle = false
    var escape = false

    for (ch in inner) {
      if (escape) {
        current.append(ch)
        escape = false
        continue
      }
      when (ch) {
        '\\' -> escape = true
        '"' -> {
          if (!inSingle) inDouble = !inDouble else current.append(ch)
        }
        '\'' -> {
          if (!inDouble) inSingle = !inSingle else current.append(ch)
        }
        ',' -> {
          if (!inDouble && !inSingle) {
            val token = cleanArgument(current.toString().trim())
            if (token.isNotEmpty()) list.add(token)
            current.clear()
          } else {
            current.append(ch)
          }
        }
        else -> current.append(ch)
      }
    }
    val finalToken = cleanArgument(current.toString().trim())
    if (finalToken.isNotEmpty()) list.add(finalToken)
    return list
  }

  private fun cleanArgument(arg: String): String {
    val trimmed = arg.trim()
    if ((trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2) ||
      (trimmed.startsWith('\'') && trimmed.endsWith('\'') && trimmed.length >= 2)
    ) {
      return trimmed.substring(1, trimmed.length - 1)
    }
    return trimmed
  }

  private fun splitStatements(raw: String): List<String> {
    val statements = mutableListOf<String>()
    val current = StringBuilder()
    var inDouble = false
    var inSingle = false
    var escape = false

    for (ch in raw) {
      if (escape) {
        current.append(ch)
        escape = false
        continue
      }
      when (ch) {
        '\\' -> {
          escape = true
          current.append(ch)
        }
        '"' -> {
          if (!inSingle) inDouble = !inDouble
          current.append(ch)
        }
        '\'' -> {
          if (!inDouble) inSingle = !inSingle
          current.append(ch)
        }
        ';', '\n', '\r' -> {
          if (!inDouble && !inSingle) {
            val stmt = current.toString().trim()
            if (stmt.isNotEmpty()) statements.add(stmt)
            current.clear()
          } else {
            current.append(ch)
          }
        }
        else -> current.append(ch)
      }
    }
    val finalStmt = current.toString().trim()
    if (finalStmt.isNotEmpty()) statements.add(finalStmt)
    return statements
  }

  fun tokenizeArguments(line: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var inDouble = false
    var inSingle = false
    var escape = false

    for (ch in line) {
      if (escape) {
        current.append(ch)
        escape = false
        continue
      }
      when (ch) {
        '\\' -> escape = true
        '"' -> {
          if (!inSingle) inDouble = !inDouble else current.append(ch)
        }
        '\'' -> {
          if (!inDouble) inSingle = !inSingle else current.append(ch)
        }
        ' ', '\t' -> {
          if (inDouble || inSingle) {
            current.append(ch)
          } else if (current.isNotEmpty()) {
            tokens.add(current.toString())
            current.clear()
          }
        }
        else -> current.append(ch)
      }
    }
    if (current.isNotEmpty()) {
      tokens.add(current.toString())
    }
    return tokens
  }
}
