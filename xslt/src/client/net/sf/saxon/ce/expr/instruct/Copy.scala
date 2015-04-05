// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
package client.net.sf.saxon.ce.expr.instruct

import client.net.sf.saxon.ce.Controller
import client.net.sf.saxon.ce.event._
import client.net.sf.saxon.ce.expr._
import client.net.sf.saxon.ce.om.Item
import client.net.sf.saxon.ce.om.NamespaceBinding
import client.net.sf.saxon.ce.om.NodeInfo
import client.net.sf.saxon.ce.om.StructuredQName
import client.net.sf.saxon.ce.pattern.NodeKindTest
import client.net.sf.saxon.ce.pattern.NodeTest
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.tree.util.NamespaceIterator
import client.net.sf.saxon.ce.`type`.AtomicType
import client.net.sf.saxon.ce.`type`.ItemType
import client.net.sf.saxon.ce.`type`.Type
import client.net.sf.saxon.ce.value.EmptySequence
import java.util.Iterator
//remove if not needed
import scala.collection.JavaConversions._

/**
 * Handler for xsl:copy elements in stylesheet.
 */
class Copy(var select: Expression, var copyNamespaces: Boolean, inheritNamespaces: Boolean)
    extends ElementCreator {

  private var resultItemType: ItemType = _

  this.inheritNamespaces = inheritNamespaces

  /**
   * Simplify an expression. This performs any static optimization (by rewriting the expression
   * as a different expression). The default implementation does nothing.
   *
   * @return the simplified expression
   * @throws client.net.sf.saxon.ce.trans.XPathException
   *          if an error is discovered during expression rewriting
   * @param visitor an expression visitor
   */
  def simplify(visitor: ExpressionVisitor): Expression = {
    select = visitor.simplify(select)
    super.simplify(visitor)
  }

  def typeCheck(visitor: ExpressionVisitor, contextItemType: ItemType): Expression = {
    try {
      select = visitor.typeCheck(select, contextItemType)
      adoptChildExpression(select)
    } catch {
      case err: XPathException ⇒
        if (err.getErrorCodeLocalPart == "XPDY0002") {
          err.setErrorCode("XTTE0945")
          err.maybeSetLocation(getSourceLocator)
        }
        select = new Literal(EmptySequence.getInstance)
        throw err
    }
    val selectItemType = select.getItemType
    if (selectItemType.isInstanceOf[NodeTest]) selectItemType.asInstanceOf[NodeTest].getRequiredNodeKind match {
      case Type.ELEMENT ⇒ this.resultItemType = NodeKindTest.ELEMENT
      case Type.ATTRIBUTE ⇒ this.resultItemType = NodeKindTest.ATTRIBUTE
      case Type.DOCUMENT ⇒ this.resultItemType = NodeKindTest.DOCUMENT
      case _ ⇒ this.resultItemType = selectItemType
    } else {
      this.resultItemType = selectItemType
    }
    super.typeCheck(visitor, contextItemType)
  }

  /**
   * Determine which aspects of the context the expression depends on. The result is
   * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
   * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
   * dependencies of this expression with the dependencies of the subexpressions,
   * computed recursively. This is overridden for expressions such as FilterExpression
   * where a subexpression's dependencies are not necessarily inherited by the parent
   * expression.
   * @return a set of bit-significant flags identifying the dependencies of
   *         the expression
   */
  def getIntrinsicDependencies(): Int = StaticProperty.DEPENDS_ON_CONTEXT_ITEM

  /**
   *  Get the immediate sub-expressions of this expression.
   * @return an iterator containing the sub-expressions of this expression
   */
  def iterateSubExpressions(): Iterator[Expression] = nonNullChildren(select, content)

  /**
   * Get the item type of the result of this instruction.
   * @return The context item type.
   */
  def getItemType(): ItemType = {
    if (resultItemType != null) {
      resultItemType
    } else {
      resultItemType = computeItemType()
      resultItemType
    }
  }

  private def computeItemType(): ItemType = select.getItemType

  def optimize(visitor: ExpressionVisitor, contextItemType: ItemType): Expression = {
    select = visitor.optimize(select, contextItemType)
    val exp = super.optimize(visitor, contextItemType)
    if (exp == this) {
      if (resultItemType == null) {
        resultItemType = computeItemType()
      }
      if (resultItemType.isInstanceOf[AtomicType]) {
        return select
      }
    }
    exp
  }

  /**
   * Evaluate as an expression. We rely on the fact that when these instructions
   * are generated by XQuery, there will always be a valueExpression to evaluate
   * the content
   */
  def evaluateItem(context: XPathContext): Item = {
    val controller = context.getController
    val c2 = context.newMinorContext()
    val seq = controller.allocateSequenceOutputter(1)
    val pipe = controller.makePipelineConfiguration()
    seq.setPipelineConfiguration(pipe)
    c2.setTemporaryReceiver(seq)
    process(c2)
    seq.close()
    val item = seq.getFirstItem
    seq.reset()
    item
  }

  /**
   * Callback from ElementCreator when constructing an element
   *
   * @param context XPath dynamic evaluation context
   * @param copiedNode the node being copied
   * @return the namecode of the element to be constructed
   * @throws XPathException
   */
  def getNameCode(context: XPathContext, copiedNode: NodeInfo): StructuredQName = copiedNode.getNodeName

  /**
   * Get the base URI of a copied element node (the base URI is retained in the new copy)
   * @param context XPath dynamic evaluation context
   * @param copiedNode
   * @return the base URI
   */
  def getNewBaseURI(context: XPathContext, copiedNode: NodeInfo): String = copiedNode.getBaseURI

  /**
   * Callback to output namespace nodes for the new element.
   *
   * @param context The execution context
   * @param receiver the Receiver where the namespace nodes are to be written
   * @param nameCode
   * @param copiedNode
   * @throws XPathException
   */
  protected def outputNamespaceNodes(context: XPathContext, 
      receiver: Receiver, 
      nameCode: StructuredQName, 
      copiedNode: NodeInfo): Unit = {
    if (copyNamespaces) {
      NamespaceIterator.sendNamespaces(copiedNode, receiver)
    } else {
      receiver.namespace(new NamespaceBinding(nameCode.getPrefix, nameCode.getNamespaceURI), 0)
    }
  }

  def processLeavingTail(context: XPathContext): TailCall = {
    val out = context.getReceiver
    val item = select.evaluateItem(context)
    if (!item.isInstanceOf[NodeInfo]) {
      out.append(item, NodeInfo.ALL_NAMESPACES)
      return null
    }
    val source = item.asInstanceOf[NodeInfo]
    source.getNodeKind match {
      case Type.ELEMENT ⇒ return super.processLeavingTail(context, item.asInstanceOf[NodeInfo])
      case Type.ATTRIBUTE ⇒ context.getReceiver.attribute(source.getNodeName, source.getStringValue)
      case Type.TEXT ⇒ out.characters(source.getStringValue)
      case Type.PROCESSING_INSTRUCTION ⇒ out.processingInstruction(source.getDisplayName, source.getStringValue)
      case Type.COMMENT ⇒ out.comment(source.getStringValue)
      case Type.NAMESPACE ⇒ try {
        source.copy(out, 0)
      } catch {
        case err: NoOpenStartTagException ⇒ dynamicError(err.getMessage, err.getErrorCodeLocalPart)
      }
      case Type.DOCUMENT ⇒
        out.startDocument()
        content.process(context)
        out.endDocument()

      case _ ⇒ throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind)
    }
    null
  }
}
