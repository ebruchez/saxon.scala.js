package client.net.sf.saxon.ce.expr

import client.net.sf.saxon.ce.trans.XPathException
//remove if not needed
import scala.collection.JavaConversions._

/**
 * A LastPositionFinder is an interface implemented by any SequenceIterator that is
 * able to return the position of the last item in the sequence.
 */
trait LastPositionFinder {

  /**
   * Get the last position (that is, the number of items in the sequence). This method is
   * non-destructive: it does not change the state of the iterator.
   * The result is undefined if the next() method of the iterator has already returned null.
   * This method returns -1 if the last position cannot be determined for this kind of iterator;
   * in this case the caller must establish the value of last() by calling getAnother() and counting
   * items in the cloned iterator.
   * @return the number of items in the sequence, or -1 if this cannot be determined.
   * @throws XPathException if a dynamic error occurs while obtaining the value.
   */
  def getLastPosition(): Int
}
