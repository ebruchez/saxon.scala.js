package client.net.sf.saxon.ce.trace

import client.net.sf.saxon.ce.om.StructuredQName
import java.util.Iterator
//remove if not needed
import scala.collection.JavaConversions._

/**
 * Information about an instruction in the stylesheet or a construct in a Query, made
 * available at run-time to a TraceListener
 */
trait InstructionInfo {

  /**
   * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
   * or it will be a constant in class {@link Location}.
   * @return an integer identifying the kind of construct
   */
  def getConstructType(): StructuredQName

  def getObjectName(): StructuredQName

  def getSystemId(): String

  /**
   * Get the line number of the instruction in the source stylesheet module.
   * If this is not known, or if the instruction is an artificial one that does
   * not relate to anything in the source code, the value returned may be -1.
   * @return the line number of the expression within the containing module
   */
  def getLineNumber(): Int

  def getProperty(name: String): AnyRef

  /**
   * Get an iterator over all the properties available. The values returned by the iterator
   * will be of type String, and each string can be supplied as input to the getProperty()
   * method to retrieve the value of the property. The iterator may return properties whose
   * value is null.
   * @return an iterator over the properties.
   */
  def getProperties(): Iterator[String]
}
