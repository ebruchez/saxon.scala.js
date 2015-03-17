// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
package client.net.sf.saxon.ce.functions

import client.net.sf.saxon.ce.expr.XPathContext
import client.net.sf.saxon.ce.om.{InscopeNamespaceResolver, Item, NodeInfo}
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.value.{AnyURIValue, StringValue}

/**
 * This class supports the function namespace-uri-for-prefix()
 */
class NamespaceForPrefix extends SystemFunction {

  def newInstance(): NamespaceForPrefix = new NamespaceForPrefix()

  /**
   * Evaluate the function
   * @param context the XPath dynamic context
   * @return the URI corresponding to the prefix supplied in the first argument, or null
   * if the prefix is not in scope
   * @throws XPathException if a failure occurs evaluating the arguments
   */
  override def evaluateItem(context: XPathContext): Item = {
    val element = argument(1).evaluateItem(context).asInstanceOf[NodeInfo]
    val p = argument(0).evaluateItem(context).asInstanceOf[StringValue]
    var prefix: String = null
    prefix = if (p == null) "" else p.getStringValue
    val resolver = new InscopeNamespaceResolver(element)
    val uri = resolver.getURIForPrefix(prefix, true)
    if (uri == null) {
      return null
    }
    new AnyURIValue(uri)
  }
}
