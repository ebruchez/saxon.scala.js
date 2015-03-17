package client.net.sf.saxon.ce.event

import client.net.sf.saxon.ce.om.StructuredQName
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.tree.util.FastStringBuffer
//remove if not needed
import scala.collection.JavaConversions._

/**
 * The CommentStripper class is a filter that removes all comments and processing instructions.
 * It also concatenates text nodes that are split by comments and PIs. This follows the rules for
 * processing stylesheets.
 * @author Michael H. Kay
 */
class CommentStripper extends ProxyReceiver {

  private var buffer: FastStringBuffer = new FastStringBuffer(FastStringBuffer.MEDIUM)

  def startElement(qName: StructuredQName, properties: Int) {
    flush()
    nextReceiver.startElement(qName, properties)
  }

  /**
   * Callback interface for SAX: not for application use
   */
  def endElement() {
    flush()
    nextReceiver.endElement()
  }

  /**
   * Handle a text node. Because we're often handling stylesheets on this path, whitespace text
   * nodes will often be stripped but we can't strip them immediately because of the case
   * [element]   [!-- comment --]text[/element], where the space before the comment is considered
   * significant. But it's worth going to some effort to avoid uncompressing the whitespace in the
   * more common case, so that it can easily be detected and stripped downstream.
   */
  def characters(chars: CharSequence) {
    buffer.append(chars)
  }

  /**
   * Remove comments
   */
  def comment(chars: CharSequence) {
  }

  /**
   * Remove processing instructions
   */
  def processingInstruction(name: String, data: CharSequence) {
  }

  /**
   * Flush the character buffer
   */
  private def flush() {
    if (buffer.length > 0) {
      nextReceiver.characters(buffer)
    }
    buffer.setLength(0)
  }
}
