package client.net.sf.saxon.ce.expr.instruct

import client.net.sf.saxon.ce.om.Sequence
import ParameterSet._
//remove if not needed
import scala.collection.JavaConversions._

object ParameterSet {

  var EMPTY_PARAMETER_SET: ParameterSet = new ParameterSet(0)

  val NOT_SUPPLIED = 0

  val SUPPLIED = 1

  val SUPPLIED_AND_CHECKED = 2
}

/**
 * A ParameterSet is a set of parameters supplied when calling a template.
 * It is a collection of id-value pairs, the ids being numeric aliases for the parameter name,
 * unique within a stylesheet
 */
class ParameterSet(capacity: Int) {

  private var keys: Array[Int] = new Array[Int](capacity)

  private var values: Array[Sequence] = new Array[Sequence](capacity)

  private var typeChecked: Array[Boolean] = new Array[Boolean](capacity)

  private var used: Int = 0

  /**
   * Create an empty parameter set
   */
  def this() {
    this(10)
  }

  /**
   * Create a parameter set as a copy of an existing parameter set
   */
  def this(existing: ParameterSet, extra: Int) {
    this(existing.used + extra)
    for (i <- 0 until existing.used) {
      put(existing.keys(i), existing.values(i), existing.typeChecked(i))
    }
  }

  /**
   * Add a parameter to the ParameterSet
   *
   * @param id The parameter id, representing its name.
   * @param value The value of the parameter
   * @param checked True if the caller has done static type checking against the required type
   */
  def put(id: Int, value: Sequence, checked: Boolean) {
    for (i <- 0 until used if keys(i) == id) {
      values(i) = value
      typeChecked(i) = checked
      return
    }
    if (used + 1 > keys.length) {
      val newlength = (if (used <= 5) 10 else used * 2)
      val newkeys = Array.ofDim[Int](newlength)
      val newChecked = Array.ofDim[Boolean](newlength)
      val newvalues = Array.ofDim[Sequence](newlength)
      System.arraycopy(values, 0, newvalues, 0, used)
      System.arraycopy(keys, 0, newkeys, 0, used)
      System.arraycopy(typeChecked, 0, newChecked, 0, used)
      values = newvalues
      keys = newkeys
    }
    keys(used) = id
    typeChecked(used) = checked
    values(used += 1) = value
  }

  /**
   * Get the index position of a parameter
   *
   * @param id The numeric parameter id, representing its name.
   * @return The index position of the parameter, or -1 if not defined
   */
  def getIndex(id: Int): Int = {
    (0 until used).find(keys(_) == id).getOrElse(-1)
  }

  /**
   * Get the value of the parameter at a given index
   */
  def getValue(index: Int): Sequence = values(index)

  /**
   * Determine whether the parameter at a given index has been type-checked
   */
  def isTypeChecked(index: Int): Boolean = typeChecked(index)

  /**
   * Clear all values
   */
  def clear() {
    used = 0
  }
}
