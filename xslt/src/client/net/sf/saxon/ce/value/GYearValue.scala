// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
package client.net.sf.saxon.ce.value

import client.net.sf.saxon.ce.functions.FormatDate
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.`type`.AtomicType
import client.net.sf.saxon.ce.`type`.ConversionResult
import client.net.sf.saxon.ce.`type`.ValidationFailure
import com.google.gwt.regexp.shared.MatchResult
import com.google.gwt.regexp.shared.RegExp
import GYearValue._
//remove if not needed
import scala.collection.JavaConversions._

object GYearValue {

  private var regex: RegExp = RegExp.compile("(-?[0-9]+)(Z|[+-][0-9][0-9]:[0-9][0-9])?")

  def makeGYearValue(value: CharSequence): ConversionResult = {
    val g = new GYearValue()
    val m = regex.exec(Whitespace.trimWhitespace(value).toString)
    if (m == null) {
      return new ValidationFailure("Cannot convert '" + value + "' to a gYear")
    }
    val base = m.getGroup(1)
    val tz = m.getGroup(2)
    val date = (if (base == null) "" else base) + "-01-01" + (if (tz == null) "" else tz)
    setLexicalValue(g, date)
  }
}

/**
 * Implementation of the xs:gYear data type
 */
class GYearValue private () extends GDateValue {

  def this(year: Int, tz: Int) {
    this()
    this.year = year
    this.month = 1
    this.day = 1
    setTimezoneInMinutes(tz)
  }

  /**
   * Make a copy of this date, time, or dateTime value
   */
  def copy(): AtomicValue = {
    new GYearValue(year, getTimezoneInMinutes)
  }

  /**
   * Determine the primitive type of the value. This delivers the same answer as
   * getItemType().getPrimitiveItemType(). The primitive types are
   * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
   * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
   */
  def getItemType(): AtomicType = AtomicType.G_YEAR

  /**
   * Convert to target data type
   *
   * @param requiredType an integer identifying the required atomic type
   * @return an AtomicValue, a value of the required type; or an ErrorValue
   */
  def convert(requiredType: AtomicType): ConversionResult = {
    if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.G_YEAR) {
      this
    } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
      new UntypedAtomicValue(getStringValue)
    } else if (requiredType == AtomicType.STRING) {
      new StringValue(getStringValue)
    } else {
      new ValidationFailure("Cannot convert gYear to " + requiredType.getDisplayName, "XPTY0004")
    }
  }

  def getPrimitiveStringValue(): CharSequence = {
    FormatDate.formatDate(this, "[Y0001][Z]", "en")
  }
}
