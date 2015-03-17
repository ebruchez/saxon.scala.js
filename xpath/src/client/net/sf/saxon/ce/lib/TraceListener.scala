package client.net.sf.saxon.ce.lib

import java.util.EventListener
import client.net.sf.saxon.ce.Controller
import client.net.sf.saxon.ce.expr.XPathContext
import client.net.sf.saxon.ce.om.Item
import client.net.sf.saxon.ce.trace.InstructionInfo
//remove if not needed
import scala.collection.JavaConversions._

/**
 * This interface defines methods that are called by Saxon during the execution of
 * a stylesheet, if tracing is switched on. Tracing can be switched on by nominating
 * an implementation of this class using the TRACE_LISTENER feature of the TransformerFactory,
 * or using the addTraceListener() method of the Controller, which is Saxon's implementation
 * of tyhe JAXP javax.xml.transform.Transformer interface.
 */
trait TraceListener extends EventListener {

  /**
   * Method called at the start of execution, that is, when the run-time transformation starts
   * @param controller identifies the transformation controller, and provides the listener with
   * access to context and configuration information
   */
  def open(): Unit

  /**
   * Method called at the end of execution, that is, when the run-time execution ends
   */
  def close(): Unit

  /**
   * Method that is called when an instruction in the stylesheet gets processed.
   *
   * @param instruction gives information about the instruction being
   *                    executed, and about the context in which it is executed. This object is mutable,
   *                    so if information from the InstructionInfo is to be retained, it must be copied.
   */
  def enter(instruction: InstructionInfo, context: XPathContext): Unit

  /**
   * Method that is called after processing an instruction of the stylesheet,
   * that is, after any child instructions have been processed.
   *
   * @param instruction gives the same information that was supplied to the
   *                    enter method, though it is not necessarily the same object. Note that the
   *                    line number of the instruction is that of the start tag in the source stylesheet,
   *                    not the line number of the end tag.
   */
  def leave(instruction: InstructionInfo): Unit

  /**
   * Method that is called by an instruction that changes the current item
   * in the source document: that is, xsl:for-each, xsl:apply-templates, xsl:for-each-group.
   * The method is called after the enter method for the relevant instruction, and is called
   * once for each item processed.
   *
   * @param currentItem the new current item. Item objects are not mutable; it is safe to retain
   *                    a reference to the Item for later use.
   */
  def startCurrentItem(currentItem: Item): Unit

  /**
   * Method that is called when an instruction has finished processing a new current item
   * and is ready to select a new current item or revert to the previous current item.
   * The method will be called before the leave() method for the instruction that made this
   * item current.
   *
   * @param currentItem the item that was current, whose processing is now complete. This will represent
   *                    the same underlying item as the corresponding startCurrentItem() call, though it will
   *                    not necessarily be the same actual object.
   */
  def endCurrentItem(currentItem: Item): Unit
}
