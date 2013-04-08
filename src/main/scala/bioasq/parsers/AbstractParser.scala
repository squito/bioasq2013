package bioasq.parsers

import io.Source
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import bioasq.DataFiles
import collection._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.Date
import org.sblaj.io.{DictionaryIO, VectorIO, VectorFileSet}
import org.sblaj.featurization.{Murmur64, MurmurFeaturizer, FeaturizerHelper, BinaryFeaturizer}
import com.quantifind.sumac.{FieldParsing, ArgMain}

/**
 *
 */

object AbstractParser extends ArgMain[ParserArgs]{

  def main(args: ParserArgs) {

    if (args.featurize) {
      val in = new GZIPInputStream(new FileInputStream(DataFiles.TrainingAbstractsGzip))
      new java.io.File(DataFiles.TrainingFeaturizedDir).mkdirs()
      featurizeAbstracts(Source.fromInputStream(in), DataFiles.TrainingFeaturizedFileSet)
    }

    if (args.mergeDictionary) {
      val dictionary = DictionaryIO.buildMergedDictionary(DataFiles.TrainingFeaturizedFileSet)
    }

    if (args.idSet) {
      val idSet = DictionaryIO.idEnumeration(DataFiles.TrainingFeaturizedFileSet)
      println(idSet.size)
    }

    if (args.enumerate) {
      VectorIO.convertToIntVectors(DataFiles.TrainingFeaturizedFileSet, DataFiles.TrainingIntVectorFileSet)
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
}

class ParserArgs extends FieldParsing {
  var featurize = false
  var mergeDictionary = false
  var idSet = false
  var enumerate = false
  var iterate = false
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
  val year: String  //for some reason, its a year in the json
)

object AbstractFeaturizer extends MurmurFeaturizer[Abstract] {
  override def getId(abs: Abstract) = Murmur64.hash64(abs.pmid)
  override def extractor(abs: Abstract) =
    abs.abstractText.split("\\s").toSet ++ //unigrams
      Set("JOURNAL:" + abs.journal) ++  //journal
      abs.meshMajor.map{"MESH:" + _}.toSet //mesh terms
}
