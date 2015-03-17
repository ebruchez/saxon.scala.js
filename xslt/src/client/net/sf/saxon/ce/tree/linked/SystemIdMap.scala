package client.net.sf.saxon.ce.tree.linked

//remove if not needed
import scala.collection.JavaConversions._

/**
 * System IDs are not held in nodes in the tree, because they are usually the same
 * for a whole document.
 * This class provides a map from element sequence numbers to System IDs: it is
 * linked to the root node of the tree.
 * Note that the System ID is not necessarily the same as the Base URI. The System ID relates
 * to the external entity in which a node was physically located; this provides a default for
 * the Base URI, but this may be modified by specifying an xml:base attribute
 *
 * @author Michael H. Kay
 */
class SystemIdMap {

  private var sequenceNumbers: Array[Int] = new Array[Int](4)

  private var uris: Array[String] = new Array[String](4)

  private var allocated: Int = 0

  /**
   * Set the system ID corresponding to a given sequence number
   */
  def setSystemId(sequence: Int, uri: String) {
    if (allocated > 0 && uri == uris(allocated - 1)) {
      return
    }
    if (sequenceNumbers.length <= allocated + 1) {
      val s = Array.ofDim[Int](allocated * 2)
      val u = Array.ofDim[String](allocated * 2)
      System.arraycopy(sequenceNumbers, 0, s, 0, allocated)
      System.arraycopy(uris, 0, u, 0, allocated)
      sequenceNumbers = s
      uris = u
    }
    sequenceNumbers(allocated) = sequence
    uris(allocated) = uri
    allocated += 1
  }

  /**
   * Get the system ID corresponding to a given sequence number
   */
  def getSystemId(sequence: Int): String = {
    if (allocated == 0) return null
    for (i <- 1 until allocated if sequenceNumbers(i) > sequence) {
      return uris(i - 1)
    }
    uris(allocated - 1)
  }
}
