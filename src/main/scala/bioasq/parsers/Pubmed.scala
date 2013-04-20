package bioasq.parsers

import java.net.URL
import xml.{Node, Elem, XML}

object Pubmed {
  val esummaryURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id="
  //eg http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=13094506,13094502
  //val esum = Pubmed.downloadMultiSummaries(List("13094506","13094502"))
  //Pubmed.extractMultipleJournals(esum)

  //TODO comma separated ids to get a big list at once
  def downloadESummary(pmid: String): Elem = {
    XML.load(new URL(esummaryURL + pmid).openStream())
  }

  def getJournalName(esummary: Elem): Option[String] = {
    val jNode = ((esummary \ "DocSum" head) \ "Item") find {child => child.attribute("Name").map{_.toString}.getOrElse("") == "FullJournalName"}
    jNode.map{_.child.head.toString}
  }

  def downloadMultiSummaries(pmids: Traversable[String]): Elem = {
    val url = new URL(esummaryURL + pmids.mkString(","))
    XML.load(url.openStream())
  }

  def extractMultipleJournals(esummary: Elem): Map[String,String] = {
    (esummary \ "DocSum").flatMap(n => extractIdAndJournal(n.asInstanceOf[Elem])).toMap
  }

  def extractIdAndJournal(docSum: Elem): Option[(String,String)] = {
    val id = (docSum \ "Id").head.child.head.toString
    val jNode = (docSum \ "Item") find {
      child: Node => child.attribute("Name").map{_.toString}.getOrElse("") == "FullJournalName"
    }
    jNode.map{id -> _.child.head.toString}
  }

}
