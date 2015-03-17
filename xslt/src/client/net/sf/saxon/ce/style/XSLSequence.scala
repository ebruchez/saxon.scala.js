package client.net.sf.saxon.ce.style

import client.net.sf.saxon.ce.LogController
import client.net.sf.saxon.ce.expr.Expression
import client.net.sf.saxon.ce.expr.instruct.Executable
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.`type`.ItemType
import com.google.gwt.logging.client.LogConfiguration
//remove if not needed
import scala.collection.JavaConversions._

/**
 * An xsl:sequence element in the stylesheet. <br>
 * The xsl:sequence element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".</li>
 * </ul>
 */
class XSLSequence extends StyleElement {

  private var select: Expression = _

  private var selectAttTrace: String = ""

  /**
   * Determine whether this node is an instruction.
   * @return true - it is an instruction
   */
  def isInstruction(): Boolean = true

  /**
   * Determine the type of item returned by this instruction (only relevant if
   * it is an instruction).
   * @return the item type returned
   */
  protected def getReturnedItemType(): ItemType = select.getItemType

  /**
   * Determine whether this type of element is allowed to contain a template-body
   * @return true: yes, it may contain a template-body
   */
  def mayContainSequenceConstructor(): Boolean = false

  def prepareAttributes() {
    select = checkAttribute("select", "e1").asInstanceOf[Expression]
    checkForUnknownAttributes()
    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
      selectAttTrace = getAttributeValue("", "select")
    }
  }

  def validate(decl: Declaration) {
    onlyAllow("fallback")
    select = typeCheck(select)
  }

  def compile(exec: Executable, decl: Declaration): Expression = {
    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
      select.AddTraceProperty("select", selectAttTrace)
    }
    select
  }
}
