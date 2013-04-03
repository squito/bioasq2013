package bioasq.parsers

import io.Source
import com.fasterxml.jackson.core.{ObjectCodec, Base64Variant, JsonParser}
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import bioasq.DataFiles
import collection._
import com.fasterxml.jackson.module.scala.DefaultScalaModule

/**
 *
 */

class AbstractParser {

}

object AbstractParser {

  def main(args: Array[String]) {
    val in = new GZIPInputStream(new FileInputStream(DataFiles.TrainingAbstractsGzip))
    parseAbstracts(Source.fromInputStream(in)){abs => println(abs + "\t" + abs.meshMajor.mkString(","))}
  }

  def parseAbstracts(file:String)(f: Abstract => Unit) {
    parseAbstracts(Source.fromFile(file))(f)
  }

  def parseAbstracts(source: Source)(f: Abstract => Unit) {
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)
    source.getLines().zipWithIndex.
      filter{case (line, idx) => idx != 0 && !line.trim.equals("}")}.
      foreach{ case (line, _) =>
        val abs = om.readValue(line, classOf[Abstract])
        f(abs)
      }
  }
}


case class Abstract(
  val abstractText: String,
  val journal: String,
  val meshMajor: Array[String],
  val pmid: String,
  val title: String,
  val year: String  //for some reason, its a year in the json
)