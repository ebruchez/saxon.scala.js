// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
package client.net.sf.saxon.ce.expr

import client.net.sf.saxon.ce.om.Item
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator

class UnfailingItemMappingIterator(base: UnfailingIterator, action: ItemMappingFunction)
    extends ItemMappingIterator(base, action) with UnfailingIterator {

  override def next(): Item = super.next()

  protected override def getBaseIterator(): UnfailingIterator = {
    super.getBaseIterator.asInstanceOf[UnfailingIterator]
  }

  override def getAnother(): UnfailingIterator = {
    val newBase = getBaseIterator.getAnother
    val action = getMappingFunction
    val newAction = if (action.isInstanceOf[StatefulMappingFunction]) action.asInstanceOf[StatefulMappingFunction].getAnother(newBase).asInstanceOf[ItemMappingFunction] else action
    new UnfailingItemMappingIterator(newBase, newAction)
  }
}
