package client.net.sf.saxon.ce.style

import client.net.sf.saxon.ce.LogController
import client.net.sf.saxon.ce.expr._
import client.net.sf.saxon.ce.expr.instruct._
import client.net.sf.saxon.ce.om.Axis
import client.net.sf.saxon.ce.om.NodeInfo
import client.net.sf.saxon.ce.om.StructuredQName
import client.net.sf.saxon.ce.pattern.AnyNodeTest
import client.net.sf.saxon.ce.pattern.NodeKindTest
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator
import client.net.sf.saxon.ce.`type`.ItemType
import client.net.sf.saxon.ce.`type`.Type
import client.net.sf.saxon.ce.value.Cardinality
import client.net.sf.saxon.ce.value.SequenceType
import client.net.sf.saxon.ce.value.StringValue
import com.google.gwt.logging.client.LogConfiguration
//remove if not needed
import scala.collection.JavaConversions._

/**
 * This class defines common behaviour across xsl:variable, xsl:param, and xsl:with-param
 */
abstract class XSLGeneralVariable extends StyleElement {

  protected var select: Expression = null

  protected var requiredType: SequenceType = null

  protected var constantText: String = null

  protected var global: Boolean = _

  protected var redundant: Boolean = false

  protected var requiredParam: Boolean = false

  protected var implicitlyRequiredParam: Boolean = false

  protected var tunnel: Boolean = false

  protected var compiledVariable: GeneralVariable = null

  private var textonly: Boolean = _

  /**
   * Determine the type of item returned by this instruction (only relevant if
   * it is an instruction).
   * @return the item type returned. This is null for a variable: we are not
   * interested in the type of the variable, but in what the xsl:variable constributes
   * to the result of the sequence constructor it is part of.
   */
  protected def getReturnedItemType(): ItemType = null

  /**
   * Determine whether this type of element is allowed to contain a template-body
   * @return true: yes, it may contain a template-body
   */
  def mayContainSequenceConstructor(): Boolean = true

  protected def allowsAsAttribute(): Boolean = true

  protected def allowsTunnelAttribute(): Boolean = false

  protected def allowsValue(): Boolean = true

  protected def allowsRequired(): Boolean = false

  /**
   * Test whether this is a tunnel parameter (tunnel="yes")
   * @return true if this is a tunnel parameter
   */
  def isTunnelParam(): Boolean = tunnel

  /**
   * Test whether this is a required parameter (required="yes")
   * @return true if this is a required parameter
   */
  def isRequiredParam(): Boolean = requiredParam

  /**
   * Test whether this is a global variable or parameter
   * @return true if this is global
   */
  def isGlobal(): Boolean = isTopLevel

  /**
   * Get the display name of the variable.
   * @return the lexical QName
   */
  def getVariableDisplayName(): String = getAttributeValue("", "name")

  /**
   * Mark this global variable as redundant. This is done before prepareAttributes is called.
   */
  def setRedundant() {
    redundant = true
  }

  /**
   * Get the QName of the variable
   * @return the name as a structured QName, or a dummy name if the variable has no name attribute
   * or has an invalid name attribute
   */
  def getVariableQName(): StructuredQName = getObjectName

  def prepareAttributes() {
    setObjectName(checkAttribute("name", "q1").asInstanceOf[StructuredQName])
    select = checkAttribute("select", "e").asInstanceOf[Expression]
    requiredType = checkAttribute("as", "z").asInstanceOf[SequenceType]
    var b = checkAttribute("required", "b").asInstanceOf[java.lang.Boolean]
    if (b != null) {
      requiredParam = b
    }
    b = checkAttribute("tunnel", "b").asInstanceOf[java.lang.Boolean]
    if (b != null) {
      tunnel = b
    }
    checkForUnknownAttributes()
    if (select != null && !allowsValue()) {
      compileError("Function parameters cannot have a default value", "XTSE0760")
    }
    if (tunnel && this.isInstanceOf[XSLParam] && !(getParent.isInstanceOf[XSLTemplate])) {
      compileError("For attribute 'tunnel' within an " + getParent.getDisplayName + 
        " parameter, the only permitted value is 'no'", "XTSE0020")
    }
  }

  def validate(decl: Declaration) {
    global = isTopLevel
    if (select != null && hasChildNodes()) {
      compileError("An " + getDisplayName + " element with a select attribute must be empty", "XTSE0620")
    }
    if (hasChildNodes() && !allowsValue()) {
      compileError("Function parameters cannot have a default value", "XTSE0760")
    }
  }

  /**
   * Hook to allow additional validation of a parent element immediately after its
   * children have been validated.
   */
  def postValidate() {
    checkAgainstRequiredType(requiredType)
    if (select == null && allowsValue()) {
      textonly = true
      val kids = iterateAxis(Axis.CHILD, AnyNodeTest.getInstance)
      val first = kids.next().asInstanceOf[NodeInfo]
      if (first == null) {
        if (requiredType == null) {
          select = new StringLiteral(StringValue.EMPTY_STRING)
        } else {
          if (this.isInstanceOf[XSLParam]) {
            if (!requiredParam) {
              if (Cardinality.allowsZero(requiredType.getCardinality)) {
                select = Literal.makeEmptySequence()
              } else {
                implicitlyRequiredParam = true
              }
            }
          } else {
            if (Cardinality.allowsZero(requiredType.getCardinality)) {
              select = Literal.makeEmptySequence()
            } else {
              compileError("The implicit value () is not valid for the declared type", "XTTE0570")
            }
          }
        }
      } else {
        if (kids.next() == null) {
          if (first.getNodeKind == Type.TEXT) {
            constantText = first.getStringValue
          }
        }
        textonly = (getCommonChildItemType == NodeKindTest.TEXT)
      }
    }
    select = typeCheck(select)
  }

  /**
   * Check the supplied select expression against the required type.
   * @param required The type required by the variable declaration, or in the case
   * of xsl:with-param, the signature of the called template
   */
  def checkAgainstRequiredType(required: SequenceType) {
    try {
      if (required != null) {
        if (select != null) {
          var category = RoleLocator.VARIABLE
          var errorCode = "XTTE0570"
          if (this.isInstanceOf[XSLParam]) {
            category = RoleLocator.PARAM
            errorCode = "XTTE0600"
          } else if (this.isInstanceOf[XSLWithParam]) {
            category = RoleLocator.PARAM
            errorCode = "XTTE0590"
          }
          val role = new RoleLocator(category, getVariableDisplayName, 0)
          role.setErrorCode(errorCode)
          select = TypeChecker.staticTypeCheck(select, required, false, role)
        } else {
        }
      }
    } catch {
      case err: XPathException => {
        err.setLocator(this)
        compileError(err)
        select = new ErrorExpression(err)
      }
    }
  }

  /**
   * Initialize - common code called from the compile() method of all subclasses
   * @param exec the executable
   * @param decl
   * @param var the representation of the variable declaration in the compiled executable
   */
  protected def initializeInstruction(exec: Executable, decl: Declaration, `var`: GeneralVariable) {
    `var`.init(select, getVariableQName)
    `var`.setRequiredParam(requiredParam)
    `var`.setImplicitlyRequiredParam(implicitlyRequiredParam)
    `var`.setRequiredType(requiredType)
    `var`.setTunnel(tunnel)
    if (hasChildNodes()) {
      if (requiredType == null) {
        val doc = new DocumentInstr(textonly, constantText, getBaseURI)
        `var`.adoptChildExpression(doc)
        var b = compileSequenceConstructor(exec, decl)
        if (b == null) {
          b = Literal.makeEmptySequence()
        }
        doc.setContentExpression(b)
        select = doc
        `var`.setSelectExpression(doc)
      } else {
        select = compileSequenceConstructor(exec, decl)
        `var`.adoptChildExpression(select)
        if (select == null) {
          select = Literal.makeEmptySequence()
        }
        try {
          if (requiredType != null) {
            `var`.setContainer(this)
            select.setContainer(this)
            val role = new RoleLocator(RoleLocator.VARIABLE, getVariableDisplayName, 0)
            role.setErrorCode("XTTE0570")
            select = makeExpressionVisitor().simplify(select)
            select = TypeChecker.staticTypeCheck(select, requiredType, false, role)
          }
        } catch {
          case err: XPathException => {
            err.setLocator(this)
            compileError(err)
            select = new ErrorExpression(err)
          }
        }
        `var`.setSelectExpression(select)
      }
    }
    if (global) {
      val gvar = `var`.asInstanceOf[GlobalVariable]
      `var`.setContainer(gvar)
      var exp2 = select
      if (exp2 != null) {
        try {
          val visitor = makeExpressionVisitor()
          exp2.setContainer(gvar)
          exp2 = visitor.typeCheck(visitor.simplify(select), Type.NODE_TYPE)
        } catch {
          case err: XPathException => compileError(err)
        }
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
          exp2 = makeTraceInstruction(this, exp2)
        }
      }
      if (exp2 != select) {
        gvar.setSelectExpression(exp2)
      }
    }
  }
}
