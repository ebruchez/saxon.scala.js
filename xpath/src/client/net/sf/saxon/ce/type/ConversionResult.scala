package client.net.sf.saxon.ce.`type`

import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.value.AtomicValue
//remove if not needed
import scala.collection.JavaConversions._

/**
 * This is a marker interface used as the result methods that convert or cast values from one type
 * to another. It is implemented by AtomicValue, which indicates a successful conversion, and by
 * ValidationFailure, which indicates an unsuccessful conversion. An unsuccessful conversion does not
 * throw an exception because exceptions are expensive and should not be used on success paths. For example
 * when validating a union, conversion failures are to be expected.
 */
trait ConversionResult {

  /**
   * Calling this method on a ConversionResult returns the AtomicValue that results
   * from the conversion if the conversion was successful, and throws a ValidationException
   * explaining the conversion error otherwise.
   *
   * <p>Use this method if you are calling a conversion method that returns a ConversionResult,
   * and if you want to throw an exception if the conversion fails.</p>
   *
   * @return the atomic value that results from the conversion if the conversion was successful
   * @throws XPathException if the conversion was not successful
   */
  def asAtomic(): AtomicValue
}
