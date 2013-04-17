package bioasq.parsers

import io.Source
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import bioasq.DataFiles
import collection._
import java.util.Date
import mutable.ArrayBuffer
import org.sblaj.io.{DictionaryIO, VectorIO, VectorFileSet}
import org.sblaj.featurization.{Murmur64, MurmurFeaturizer, FeaturizerHelper, BinaryFeaturizer}
import com.quantifind.sumac.{FieldArgs, ArgMain}
import dataingest.JsonToLdaC
import com.fasterxml.jackson.module.scala.DefaultScalaModule

/**
 *
 */

object AbstractParser extends ArgMain[ParserArgs]{

  def main(args: ParserArgs) {

    if (args.featurize) {
      val in = new GZIPInputStream(new FileInputStream(DataFiles.TrainingAbstractsGzip))
      new java.io.File(DataFiles.trainingFeaturizedDir(args.featureSetName)).mkdirs()
      featurizeAbstracts(Source.fromInputStream(in), DataFiles.trainingFeaturizedFileSet(args.featureSetName))
    }

    if (args.toIntVectors) {
      VectorIO.convertToIntVectorsWithPredicate(
        DataFiles.trainingFeaturizedFileSet(args.featureSetName),
        DataFiles.trainingIntVectorFileSet(args.featureSetName),
        args.minFrac,
        AbstractFeaturizer.preserveFeaturePredicate _
      )
    }

    if (args.testSet != null) {
      val testSet = parseTestAbstracts(DataFiles.testSetAbstractJson("1"))
      println("testSet size = " + testSet.size)
    }
  }

  def featurizeAbstracts(abstractSource: Source, featurizationFiles: VectorFileSet) {
    val abstractsItr = new ItrWithLogs[Abstract](parseAbstracts(abstractSource),max=Int.MaxValue,logF = {
      (abs, idx) => if (idx % 10000 == 0) println(new Date() + "\t" + idx)
    })
    FeaturizerHelper.featurizeToFiles(abstractsItr, AbstractFeaturizer, featurizationFiles, 1e5.toInt)
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

}

class ParserArgs extends FieldArgs {
  var featurize = false
  var toIntVectors = false
  var featureSetName: String = _
  var minFrac = 1e-5
  var testSet: String = _
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
}

case class TestAbstract(
  val pmid: String,
  val title: String,
  val `abstract`: String
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