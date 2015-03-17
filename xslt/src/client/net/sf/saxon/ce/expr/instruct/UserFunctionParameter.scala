package client.net.sf.saxon.ce.expr.instruct

import client.net.sf.saxon.ce.expr.Binding
import client.net.sf.saxon.ce.expr.XPathContext
import client.net.sf.saxon.ce.om.StructuredQName
import client.net.sf.saxon.ce.om.Sequence
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.value.SequenceType
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

/**
 * Run-time object representing a formal argument to a user-defined function
 */
class UserFunctionParameter extends Binding {

  @BeanProperty
  var requiredType: SequenceType = _

  @BeanProperty
  var variableQName: StructuredQName = _

  private var slotNumber: Int = _

  /**
   * Indicate whether the binding is local or global. A global binding is one that has a fixed
   * value for the life of a query or transformation; any other binding is local.
   * @return false (always)
   */
  def isGlobal(): Boolean = false

  /**
   * Set the slot number to be used by this parameter
   * @param slot the slot number, that is, the position of the parameter value within the local stack frame
   */
  def setSlotNumber(slot: Int) {
    slotNumber = slot
  }

  /**
   * If this is a local variable held on the local stack frame, return the corresponding slot number.
   * In other cases, return -1.
   * @return the slot number, indicating the position of the parameter on the local stack frame
   */
  def getLocalSlotNumber(): Int = slotNumber

  /**
   * Evaluate this function parameter
   * @param context the XPath dynamic context
   * @return the value of the parameter
   * @throws XPathException if an error occurs
   */
  def evaluateVariable(context: XPathContext): Sequence = {
    context.evaluateLocalVariable(slotNumber)
  }
}
