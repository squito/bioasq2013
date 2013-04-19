package bioasq.parsers

import java.net.URL
import xml.{Elem, XML}

object Pubmed {
  val esummaryURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id="

  def downloadESummary(pmid: String): Elem = {
    XML.load(new URL(esummaryURL + pmid).openStream())
  }

  def getJournalName(esummary: Elem): Option[String] = {
    val jNode = ((esummary \ "DocSum" head) \ "Item") find {child => child.attribute("Name").map{_.toString}.getOrElse("") == "FullJournalName"}
    jNode.map{_.child.head.toString}
  }

}
