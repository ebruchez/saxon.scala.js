package client.net.sf.saxon.ce.functions.codenorm

import client.net.sf.saxon.ce.Configuration
import client.net.sf.saxon.ce.om.Axis
import client.net.sf.saxon.ce.om.DocumentInfo
import client.net.sf.saxon.ce.om.NodeInfo
import client.net.sf.saxon.ce.pattern.NodeKindTest
import client.net.sf.saxon.ce.trans.XPathException
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator
import client.net.sf.saxon.ce.value.Whitespace
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
//remove if not needed
import scala.collection.JavaConversions._

object UnicodeDataParserFromXML {

  /**
   * Called exactly once by NormalizerData to build the static data
   */
  def build(config: Configuration): NormalizerData = {
    val doc = config.buildDocument("normalizationData.xml")
    val isExcluded = new BitSet(128000)
    val isCompatibility = new BitSet(128000)
    var canonicalClassKeys: NodeInfo = null
    var canonicalClassValues: NodeInfo = null
    var decompositionKeys: NodeInfo = null
    var decompositionValues: NodeInfo = null
    val iter = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT)
    while (true) {
      val item = iter.next().asInstanceOf[NodeInfo]
      if (item == null) {
        //break
      }
      if (item.getLocalPart == "CanonicalClassKeys") {
        canonicalClassKeys = item
      } else if (item.getLocalPart == "CanonicalClassValues") {
        canonicalClassValues = item
      } else if (item.getLocalPart == "DecompositionKeys") {
        decompositionKeys = item
      } else if (item.getLocalPart == "DecompositionValues") {
        decompositionValues = item
      } else if (item.getLocalPart == "ExclusionList") {
        readExclusionList(item.getStringValue, isExcluded)
      } else if (item.getLocalPart == "CompatibilityList") {
        readCompatibilityList(item.getStringValue, isCompatibility)
      }
    }
    val canonicalClass = new HashMap[Integer, Integer](400)
    readCanonicalClassTable(canonicalClassKeys.getStringValue, canonicalClassValues.getStringValue, canonicalClass)
    val decompose = new HashMap[Integer, String](18000)
    val compose = new HashMap[Integer, Integer](15000)
    readDecompositionTable(decompositionKeys.getStringValue, decompositionValues.getStringValue, decompose, 
      compose, isExcluded, isCompatibility)
    new NormalizerData(canonicalClass, decompose, compose, isCompatibility, isExcluded)
  }

  /**
   * Reads exclusion list and stores the data
   */
  private def readExclusionList(s: String, isExcluded: BitSet) {
    for (tok <- Whitespace.tokenize(s)) {
      val value = Integer.parseInt(tok, 32)
      isExcluded.set(value)
    }
  }

  /**
   * Reads compatibility list and stores the data
   */
  private def readCompatibilityList(s: String, isCompatible: BitSet) {
    for (tok <- Whitespace.tokenize(s)) {
      val value = Integer.parseInt(tok, 32)
      isCompatible.set(value)
    }
  }

  /**
   * Read canonical class table (mapping from character codes to their canonical class)
   */
  private def readCanonicalClassTable(keyString: String, valueString: String, canonicalClasses: Map[Integer, Integer]) {
    val keys = new ArrayList(5000)
    for (tok <- Whitespace.tokenize(keyString)) {
      val value = Integer.parseInt(tok, 32)
      keys.add(value)
    }
    var k = 0
    for (tok <- Whitespace.tokenize(valueString)) {
      var clss: Int = 0
      var repeat = 1
      val star = tok.indexOf('*')
      if (star < 0) {
        clss = Integer.parseInt(tok, 32)
      } else {
        repeat = Integer.parseInt(tok.substring(0, star))
        clss = Integer.parseInt(tok.substring(star + 1), 32)
      }
      for (i <- 0 until repeat) {
        canonicalClasses.put(keys.get(k += 1).asInstanceOf[java.lang.Integer].intValue(), clss)
      }
    }
  }

  /**
   * Read canonical class table (mapping from character codes to their canonical class)
   */
  private def readDecompositionTable(decompositionKeyString: String, 
      decompositionValuesString: String, 
      decompose: Map[Integer, String], 
      compose: Map[Integer, Integer], 
      isExcluded: BitSet, 
      isCompatibility: BitSet) {
    val k = 0
    val values = new ArrayList[String](1000)
    for (tok <- Whitespace.tokenize(decompositionValuesString)) {
      var value = ""
      var c = 0
      while (c < tok.length) {
        val h0 = tok.charAt(c += 1)
        val h1 = tok.charAt(c += 1)
        val h2 = tok.charAt(c += 1)
        val h3 = tok.charAt(c += 1)
        val code = ("0123456789abcdef".indexOf(h0) << 12) + ("0123456789abcdef".indexOf(h1) << 8) + 
          ("0123456789abcdef".indexOf(h2) << 4) + 
          ("0123456789abcdef".indexOf(h3))
        value += code.toChar
      }
      values.add(value)
    }
    for (tok <- Whitespace.tokenize(decompositionKeyString)) {
      val key = Integer.parseInt(tok, 32)
      val value = values.get(k += 1)
      decompose.put(key, value)
      if (!isCompatibility.get(key) && !isExcluded.get(key)) {
        var first = ' '
        var second = value.charAt(0)
        if (value.length > 1) {
          first = second
          second = value.charAt(1)
        }
        val pair = (first << 16) | second
        compose.put(pair, key)
      }
    }
    for (SIndex <- 0 until SCount) {
      val TIndex = SIndex % TCount
      var first: Char = 0
      var second: Char = 0
      if (TIndex != 0) {
        first = (SBase + SIndex - TIndex).toChar
        second = (TBase + TIndex).toChar
      } else {
        first = (LBase + SIndex / NCount).toChar
        second = (VBase + (SIndex % NCount) / TCount).toChar
      }
      val pair = (first << 16) | second
      val key = SIndex + SBase
      decompose.put(key, String.valueOf(first) + second)
      compose.put(pair, key)
    }
  }

  /**
   * Hangul composition constants
   */
  private val SBase = 0xAC00

  private val LBase = 0x1100

  private val VBase = 0x1161

  private val TBase = 0x11A7

  private val LCount = 19

  private val VCount = 21

  private val TCount = 28

  private val NCount = VCount * TCount

  private val SCount = LCount * NCount
}
