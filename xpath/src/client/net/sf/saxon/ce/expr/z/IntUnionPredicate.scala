package client.net.sf.saxon.ce.expr.z

//remove if not needed
import scala.collection.JavaConversions._

/**
 * An IntPredicate formed as the union of two other predicates: it matches
 * an integer if either of the operands matches the integer
 */
class IntUnionPredicate(var p1: IntPredicate, var p2: IntPredicate) extends IntPredicate {

  /**
   * Ask whether a given value matches this predicate
   *
   * @param value the value to be tested
   * @return true if the value matches; false if it does not
   */
  def matches(value: Int): Boolean = p1.matches(value) || p2.matches(value)

  /**
   * Get the operands
   * @return an array containing the two operands
   */
  def getOperands(): Array[IntPredicate] = Array(p1, p2)
}
