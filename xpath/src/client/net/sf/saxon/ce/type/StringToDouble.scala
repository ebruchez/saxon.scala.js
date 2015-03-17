package client.net.sf.saxon.ce.`type`

import client.net.sf.saxon.ce.value.Whitespace
//remove if not needed
import scala.collection.JavaConversions._

object StringToDouble {

  /**
   * Convert a string to a double.
   * @param s the String to be converted
   * @return a double representing the value of the String
   * @throws NumberFormatException if the value cannot be converted
   */
  def stringToNumber(s: CharSequence): Double = {
    var containsDisallowedChars = false
    var containsWhitespace = false
    for (i <- 0 until s.length) {
      val c = s.charAt(i)
      c match {
        case ' ' | '\n' | '\t' | '\r' => containsWhitespace = true
        case 'x' | 'X' | 'f' | 'F' | 'd' | 'D' | 'n' | 'N' => containsDisallowedChars = true
        case _ => //break
      }
    }
    val n = (if (containsWhitespace) Whitespace.trimWhitespace(s).toString else s.toString)
    if ("INF" == n) {
      Double.POSITIVE_INFINITY
    } else if ("-INF" == n) {
      Double.NEGATIVE_INFINITY
    } else if ("NaN" == n) {
      Double.NaN
    } else if (containsDisallowedChars) {
      throw new NumberFormatException("invalid floating point value: " + s)
    } else {
      Double.parseDouble(n)
    }
  }
}
