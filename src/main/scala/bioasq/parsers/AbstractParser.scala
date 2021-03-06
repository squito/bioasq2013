package bioasq.parsers

import io.Source
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.zip.GZIPInputStream
import java.io.{File, FileInputStream}
import bioasq.DataFiles
import collection._
import java.util.Date
import mutable.ArrayBuffer
import org.sblaj.io.{DictionaryIO, VectorIO, VectorFileSet}
import org.sblaj.featurization.{Murmur64, MurmurFeaturizer, FeaturizerHelper, BinaryFeaturizer}
import com.quantifind.sumac.{FieldArgs, ArgMain}
import dataingest.JsonToLdaC
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util
import collection.JavaConverters._
import org.sblaj.util.Logging
import it.unimi.dsi.fastutil.longs.Long2IntMap


/**
 *
 */

object AbstractParser extends ArgMain[ParserArgs] with Logging {

  def main(args: ParserArgs) {

    if (args.featurize) {
      val in = new GZIPInputStream(new FileInputStream(DataFiles.TrainingAbstractsGzip))
      new java.io.File(DataFiles.trainingFeaturizedDir(args.featureSetName)).mkdirs()
      featurizeAbstracts(Source.fromInputStream(in), DataFiles.trainingFeaturizedFileSet(args.featureSetName), args.minYear)
    }

    if (args.toIntVectors) {
      VectorIO.convertToIntVectorsWithPredicate(
        DataFiles.trainingFeaturizedFileSet(args.featureSetName),
        DataFiles.trainingIntVectorFileSet(args.featureSetName),
        args.minFrac,
        AbstractFeaturizer.preserveFeaturePredicate _
      )
    }

    if (args.addJournalToTestSet != null) {
      val testSetNoJournal = parseTestAbstracts(DataFiles.testSetAbstractJsonNoJournal(args.addJournalToTestSet))
      val testSetWithJournal = new ArrayBuffer[TestAbstract]
      testSetNoJournal.grouped(500).foreach{absGroup =>
        Thread.sleep(500)  //dont' overload the pubmed servers
        val pmids = absGroup.map{_.pmid}
        val pmidToJournal = Pubmed.extractMultipleJournals(Pubmed.downloadMultiSummaries(pmids))
        info("got journal for " + pmidToJournal.size + " out of " + pmids.size)
        absGroup.foreach{abs =>
          testSetWithJournal += abs.copy(journal = pmidToJournal.get(abs.pmid))
        }
      }
      val om = new ObjectMapper()
      om.registerModule(DefaultScalaModule)
      om.writeValue(
        new File(DataFiles.testSetAbstractJson(args.addJournalToTestSet)),
        new TestSet(documents=testSetWithJournal)
      )
    }
  }

  def featurizeAbstracts(abstractSource: Source, featurizationFiles: VectorFileSet, minYear: Int) {
    val yearCounts = new util.TreeMap[Int,Int]().asScala
    val abstractsItr = new ItrWithLogs[Abstract](parseAbstracts(abstractSource),max=Int.MaxValue,logF = {
      (abs, idx) => if (idx % 10000 == 0) println(new Date() + "\t" + (idx / 1000) + "K")
    }).filter{t => {
      val y =  try{t.year.toInt} catch{ case ex: Exception => 0}
      yearCounts(y) = yearCounts.getOrElse(y, 0) + 1
      y >= minYear
    }}
    FeaturizerHelper.featurizeToFiles(abstractsItr, AbstractFeaturizer, featurizationFiles, 1e5.toInt)


    info("Abstracts per year:")
    yearCounts.foreach{case(year,counts)=>
      info(year + "\t" + counts)
    }
  }

  def parseAbstracts(file:String)(f: Abstract => Unit) {
    parseAbstracts(Source.fromFile(file)).foreach(f)
  }

  def parseAbstracts(source: Source): Iterator[Abstract] = {
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)
    source.getLines().zipWithIndex.
      filter{case (line, idx) => idx != 0 && !line.trim.equals("]}")}.
      map{ case (line, idx) =>
        om.readValue(line, classOf[Abstract])
      }
  }

  def parseTestAbstracts(file: String): Seq[TestAbstract] = {
    parseTestAbstracts(Source.fromFile(file))
  }

  //test sets are small -- just load them into memory
  def parseTestAbstracts(source: Source): Seq[TestAbstract] = {
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)
    om.readValue(source.getLines().next(), classOf[TestSet]).documents
  }

  def writeResults(labeled: LabeledAbstractSet, out: String) {
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)
    om.writeValue(new File(out), labeled)
  }
}

class ParserArgs extends FieldArgs {
  var featurize = false
  var toIntVectors = false
  var featureSetName: String = _
  var minFrac = 1e-5
  var addJournalToTestSet: String = _
  var minYear: Int = 2000
}


class ItrWithLogs[T](val base: Iterator[T], val max: Int = Int.MaxValue, logF: (T,Int) => Unit) extends Iterator[T] {
  var idx = 0
  def next = {
    val n = base.next()
    logF(n, idx)
    idx += 1
    n
  }
  def hasNext = {idx < max && base.hasNext}
}


case class Abstract(
  val abstractText: String,
  val journal: String,
  val meshMajor: Array[String],
  val pmid: String,
  val title: String,
  val year: String  //for some reason, its a String in the json
)

object AbstractFeaturizer extends MurmurFeaturizer[Abstract] {

  val JOURNAL_PREFIX = "JOURNAL:"
  val JOURNAL_WORD_PREFIX = "JOURNAL_WORDS:"
  val MESH_PREFIX = "MESH:"

  override def getId(abs: Abstract) = Murmur64.hash64(abs.pmid)
  override def extractor(abs: Abstract) =
    unigrams(abs.abstractText) ++ //unigrams
      Set(JOURNAL_PREFIX + abs.journal) ++  //journal
      unigrams(abs.journal).map{JOURNAL_WORD_PREFIX + _} ++ //words in journal title
      abs.meshMajor.map{MESH_PREFIX + _}.toSet //mesh terms

  def unigrams(text: String): Set[String] = {
    text.replaceAll("[^A-Za-z0-9 ]", "").split("\\s").map{_.toLowerCase}.toSet.filterNot{w =>
      JsonToLdaC.stopwords.contains(w) || w.length() < 2
    }

  }

  def preserveFeaturePredicate(f: (String,Long)): Boolean = {
    f._1.startsWith(JOURNAL_PREFIX) ||
      f._1.startsWith(JOURNAL_WORD_PREFIX) ||
      f._1.startsWith(MESH_PREFIX)
  }

  def testAbtractFeaturize(abs: TestAbstract, featuresInSet: Long2IntMap, writeInto: Array[Int]): Int = {
    val codes = unigrams(abs.`abstract`).map{s => featuresInSet.get(Murmur64.hash64(s))}
    var idx = 0
    codes.foreach{c =>
      writeInto(idx) = c
      idx += 1
    }
    java.util.Arrays.sort(writeInto, 0, idx)
    idx
  }
}

case class TestAbstract(
  val pmid: String,
  val title: String,
  val `abstract`: String,
  val journal: Option[String]
  //not sure if there is an implicit "year" as well
)

case class TestSet(
  val documents: ArrayBuffer[TestAbstract]
)

case class LabeledAbstract(
  val labels: ArrayBuffer[String],
  val pmid: String
)

case class LabeledAbstractSet(
  val documents: ArrayBuffer[LabeledAbstract]
)