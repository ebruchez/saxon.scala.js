package client.net.sf.saxon.ce.pattern

import client.net.sf.saxon.ce.om.NodeInfo
import client.net.sf.saxon.ce.om.StructuredQName
import client.net.sf.saxon.ce.`type`.ItemType
import client.net.sf.saxon.ce.`type`.Type
//remove if not needed
import scala.collection.JavaConversions._

/**
 * NodeTest is an interface that enables a test of whether a node has a particular
 * name and type. A NameTest matches the node kind and the namespace URI and the local
 * name.
 *
 * @author Michael H. Kay
 */
class NameTest(var nodeKind: Int, uri: String, localName: String) extends NodeTest {

  private var qName: StructuredQName = new StructuredQName("", uri, localName)

  /**
   * Create a NameTest to match nodes by their nameCode allocated from the NamePool
   * @param nodeKind the kind of node, for example {@link Type#ELEMENT}
   * @param qName the required name of the node
   */
  def this(nodeKind: Int, qName: StructuredQName) {
    this()
    this.nodeKind = nodeKind
    this.qName = qName
  }

  /**
   * Create a NameTest for nodes of the same type and name as a given node
   * @param node the node whose node kind and node name will form the basis of the NameTest
   */
  def this(node: NodeInfo) {
    this()
    this.nodeKind = node.getNodeKind
    this.qName = node.getNodeName
  }

  /**
   * Test whether this node test is satisfied by a given node
   * @param nodeKind The type of node to be matched
   * @param qName identifies the expanded name of the node to be matched
   */
  def matches(nodeKind: Int, qName: StructuredQName): Boolean = {
    nodeKind == this.nodeKind && qName == qName
  }

  /**
   * Test whether this node test is satisfied by a given node. This alternative
   * method is used in the case of nodes where calculating the fingerprint is expensive,
   * for example DOM or JDOM nodes.
   * @param node the node to be matched
   */
  def matches(node: NodeInfo): Boolean = {
    if (node.getNodeKind != nodeKind) {
      return false
    }
    val name = node.getNodeName
    (if (name == null) qName == null else name == qName)
  }

  /**
   * Test whether the NameTest matches a given QName
   * @param name the QName to be matched
   * @return true if the name matches
   */
  def matches(name: StructuredQName): Boolean = {
    (if (name == null) qName == null else name == qName)
  }

  /**
   * Determine the default priority of this node test when used on its own as a Pattern
   */
  def getDefaultPriority(): Double = 0.0

  /**
   * Get the fingerprint required
   */
  def getRequiredNodeName(): StructuredQName = qName

  /**
   * Determine the types of nodes to which this pattern applies. Used for optimisation.
   * For patterns that match nodes of several types, return Type.NODE
   * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
   */
  def getRequiredNodeKind(): Int = nodeKind

  /**
   * Get the type from which this item type is derived by restriction. This
   * is the supertype in the XPath type heirarchy, as distinct from the Schema
   * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
   * whose supertype is item() (rather than xs:anySimpleType).
   * <p>
   * In fact the concept of "supertype" is not really well-defined, because the types
   * form a lattice rather than a hierarchy. The only real requirement on this function
   * is that it returns a type that strictly subsumes this type, ideally as narrowly
   * as possible.
   * @return the supertype, or null if this type is item()
   */
  def getSuperType(): ItemType = NodeKindTest.makeNodeKindTest(nodeKind)

  /**
   * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
   * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
   */
  def getNodeKindMask(): Int = 1 << nodeKind

  override def toString(): String = nodeKind match {
    case Type.ELEMENT => "element(" + qName.getClarkName + ")"
    case Type.ATTRIBUTE => "attribute(" + qName.getClarkName + ")"
    case Type.PROCESSING_INSTRUCTION => "processing-instruction(" + qName.getLocalName + ')'
    case Type.NAMESPACE => "namespace(" + qName.getLocalName + ')'
    case _ => qName.getDisplayName
  }

  /**
   * Returns a hash code value for the object.
   */
  override def hashCode(): Int = nodeKind << 20 ^ qName.hashCode

  /**
   * Determines whether two NameTests are equal
   */
  override def equals(other: Any): Boolean = {
    other.isInstanceOf[NameTest] && other.asInstanceOf[NameTest].nodeKind == nodeKind && 
      other.asInstanceOf[NameTest].qName == qName
  }
}
