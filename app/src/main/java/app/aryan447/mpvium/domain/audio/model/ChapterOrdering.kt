package app.aryan447.mpvium.domain.audio.model

import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import java.util.Locale

object ChapterOrdering {

  /**
   * Natural order comparator that prefers trackNumber when both tracks have a positive
   * distinct trackNumber, otherwise falling back to natural alphanumeric comparison on title/displayName.
   */
  fun compare(trackA: LocalAudioTrack, trackB: LocalAudioTrack): Int {
    if (trackA.trackNumber > 0 && trackB.trackNumber > 0 && trackA.trackNumber != trackB.trackNumber) {
      return trackA.trackNumber.compareTo(trackB.trackNumber)
    }

    val nameA = trackA.title.ifBlank { trackA.displayName }
    val nameB = trackB.title.ifBlank { trackB.displayName }
    return naturalCompare(nameA, nameB)
  }

  /**
   * Compares two strings using natural sorting, so "Chapter 2" precedes "Chapter 10".
   */
  fun naturalCompare(a: String, b: String): Int {
    val lenA = a.length
    val lenB = b.length
    var i = 0
    var j = 0

    while (i < lenA && j < lenB) {
      val cA = a[i]
      val cB = b[j]

      if (cA.isDigit() && cB.isDigit()) {
        var startA = i
        while (startA < lenA && a[startA] == '0') startA++
        var endA = startA
        while (endA < lenA && a[endA].isDigit()) endA++

        var startB = j
        while (startB < lenB && b[startB] == '0') startB++
        var endB = startB
        while (endB < lenB && b[endB].isDigit()) endB++

        val numDigitsA = endA - startA
        val numDigitsB = endB - startB

        if (numDigitsA != numDigitsB) {
          return numDigitsA.compareTo(numDigitsB)
        }

        for (k in 0 until numDigitsA) {
          val digitDiff = a[startA + k] - b[startB + k]
          if (digitDiff != 0) return digitDiff
        }

        // Numbers were equal (e.g. 01 vs 1); advance past all digits
        var rawEndA = i
        while (rawEndA < lenA && a[rawEndA].isDigit()) rawEndA++
        var rawEndB = j
        while (rawEndB < lenB && b[rawEndB].isDigit()) rawEndB++

        i = rawEndA
        j = rawEndB
      } else {
        val diff = cA.lowercaseChar().compareTo(cB.lowercaseChar())
        if (diff != 0) return diff
        i++
        j++
      }
    }

    return (lenA - i).compareTo(lenB - j)
  }
}
