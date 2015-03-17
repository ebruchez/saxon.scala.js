package client.net.sf.saxon.ce.expr

import client.net.sf.saxon.ce.om.Item
import client.net.sf.saxon.ce.om.SequenceIterator
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.tree.iter.OneItemGoneIterator
import client.net.sf.saxon.ce.`type`.ItemType
import client.net.sf.saxon.ce.value.Cardinality
import CardinalityChecker._
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

object CardinalityChecker {

  /**
   * Factory method to construct a CardinalityChecker. The method may create an expression that combines
   * the cardinality checking with the functionality of the underlying expression class
   * @param sequence the base sequence whose cardinality is to be checked
   * @param cardinality the required cardinality
   * @param role information to be used in error reporting
   * @return a new Expression that does the CardinalityChecking (not necessarily a CardinalityChecker)
   */
  def makeCardinalityChecker(sequence: Expression, cardinality: Int, role: RoleLocator): Expression = {
    val result = new CardinalityChecker(sequence, cardinality, role)
    ExpressionTool.copyLocationInfo(sequence, result)
    result
  }
}

/**
 * A CardinalityChecker implements the cardinality checking of "treat as": that is,
 * it returns the supplied sequence, checking that its cardinality is correct
 */
class CardinalityChecker private (sequence: Expression, @BeanProperty var requiredCardinality: Int, var role: RoleLocator)
    extends UnaryExpression(sequence) {

  computeStaticProperties()

  adoptChildExpression(sequence)

  /**
   * Type-check the expression
   */
  def typeCheck(visitor: ExpressionVisitor, contextItemType: ItemType): Expression = {
    operand = visitor.typeCheck(operand, contextItemType)
    if (requiredCardinality == StaticProperty.ALLOWS_ZERO_OR_MORE || 
      Cardinality.subsumes(requiredCardinality, operand.getCardinality)) {
      return operand
    }
    this
  }

  /**
   * Perform optimisation of an expression and its subexpressions.
   * <p/>
   * <p>This method is called after all references to functions and variables have been resolved
   * to the declaration of the function or variable, and after all type checking has been done.</p>
   *
   * @param visitor an expression visitor
   * @param contextItemType the static type of "." at the point where this expression is invoked.
   *                        The parameter is set to null if it is known statically that the context item will be undefined.
   *                        If the type of the context item is not known statically, the argument is set to
   *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
   * @return the original expression, rewritten if appropriate to optimize execution
   * @throws XPathException if an error is discovered during this phase
   *                                        (typically a type error)
   */
  def optimize(visitor: ExpressionVisitor, contextItemType: ItemType): Expression = {
    operand = visitor.optimize(operand, contextItemType)
    if (requiredCardinality == StaticProperty.ALLOWS_ZERO_OR_MORE || 
      Cardinality.subsumes(requiredCardinality, operand.getCardinality)) {
      return operand
    }
    this
  }

  /**
   * Set the error code to be returned (this is used when evaluating the functions such
   * as exactly-one() which have their own error codes)
   * @param code the error code to be used
   */
  def setErrorCode(code: String) {
    role.setErrorCode(code)
  }

  /**
   * Get the RoleLocator, which contains diagnostic information for use if the cardinality check fails
   * @return the diagnostic information
   */
  def getRoleLocator(): RoleLocator = role

  /**
   * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
   * This method indicates which of these methods is provided. This implementation provides both iterate() and
   * process() methods natively.
   */
  def getImplementationMethod(): Int = {
    var m = ITERATE_METHOD
    if (!Cardinality.allowsMany(requiredCardinality)) {
      m |= EVALUATE_METHOD
    }
    m
  }

  /**
   * Iterate over the sequence of values
   */
  def iterate(context: XPathContext): SequenceIterator = {
    var base = operand.iterate(context)
    if (!Cardinality.allowsZero(requiredCardinality)) {
      val first = base.next()
      if (first == null) {
        typeError("An empty sequence is not allowed as the " + role.getMessage, role.getErrorCode)
      } else {
        base = new OneItemGoneIterator(first, base)
      }
    }
    if (Cardinality.allowsMany(requiredCardinality)) {
      base
    } else {
      val function = new SingletonCheckingFunction()
      new ItemMappingIterator(base, function)
    }
  }

  class SingletonCheckingFunction extends ItemMappingFunction with StatefulMappingFunction {

    var count: Int = 0

    def mapItem(item: Item): Item = {
      if (count > 1) {
        typeError("A sequence of more than one item is not allowed as the " + 
          role.getMessage, role.getErrorCode)
        null
      } else {
        item
      }
    }

    /**
     * Return a clone of this MappingFunction, with the state reset to its state at the beginning
     * of the underlying iteration
     *
     * @return a clone of this MappingFunction
     * @param newBaseIterator
     */
    def getAnother(newBaseIterator: SequenceIterator): StatefulMappingFunction = new SingletonCheckingFunction()
  }

  /**
   * Evaluate as an Item.
   */
  def evaluateItem(context: XPathContext): Item = {
    val iter = operand.iterate(context)
    var item: Item = null
    while (true) {
      val nextItem = iter.next()
      if (nextItem == null) //break
      if (requiredCardinality == StaticProperty.EMPTY) {
        typeError("An empty sequence is required as the " + role.getMessage, role.getErrorCode)
        return null
      }
      if (item != null) {
        typeError("A sequence of more than one item is not allowed as the " + 
          role.getMessage, role.getErrorCode)
        return null
      }
      item = nextItem
    }
    if (item == null && !Cardinality.allowsZero(requiredCardinality)) {
      typeError("An empty sequence is not allowed as the " + role.getMessage, role.getErrorCode)
      return null
    }
    item
  }

  /**
   * Determine the data type of the items returned by the expression, if possible
   * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
   * or Type.ITEM (meaning not known in advance)
   */
  def getItemType(): ItemType = operand.getItemType

  /**
   * Determine the static cardinality of the expression
   */
  def computeCardinality(): Int = requiredCardinality

  /**
   * Get the static properties of this expression (other than its type). The result is
   * bit-signficant. These properties are used for optimizations. In general, if
   * property bit is set, it is true, but if it is unset, the value is unknown.
   */
  def computeSpecialProperties(): Int = operand.getSpecialProperties

  /**
   * Is this expression the same as another expression?
   */
  override def equals(other: Any): Boolean = {
    super == other && 
      requiredCardinality == 
      other.asInstanceOf[CardinalityChecker].requiredCardinality
  }

  /**
   * get HashCode for comparing two expressions. Note that this hashcode gives the same
   * result for (A op B) and for (B op A), whether or not the operator is commutative.
   */
  override def hashCode(): Int = super.hashCode ^ requiredCardinality
}
