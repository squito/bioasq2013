package bioasq.evaluation

import com.quantifind.sumac.{ArgMain, FieldArgs}


import bioasq.DataFiles._
import bioasq.evaluation._
import bioasq.ml._
import org.sblaj._
import org.sblaj.io._
import collection.JavaConverters._
import org.sblaj.featurization.ArrayCodeLookup
import org.sblaj.util.Logging
import bioasq.parsers.AbstractFeaturizer


/**
 *
 */

object RunEvaluation extends ArgMain[RunEvaluationArgs] with Logging {
  def main(args: RunEvaluationArgs) {
    val fileSet = trainingIntVectorFileSet(args.featureSetName).sampleSizeLimitIntVectors(args.maxBytes)
    val mat = VectorIO.loadMatrix(fileSet)

    val codeLookup = ArrayCodeLookup.loadFromText(mat.nCols, scala.io.Source.fromFile(fileSet.getMergedDictionaryFile))
    val revLookup = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[String]()
    (0 until codeLookup.arr.size).foreach{idx => revLookup.put(codeLookup(idx), idx)}

    val meshIdMap = revLookup.asScala.filter{case(k,v) => k.startsWith(AbstractFeaturizer.MESH_PREFIX)}
    val meshIds = meshIdMap.map{_._2.toInt}.toArray.sorted

    val allJournalIdMap = revLookup.asScala.filter{case(k,v) => k.startsWith(AbstractFeaturizer.JOURNAL_PREFIX)}
    val allJournalIds = allJournalIdMap.map{_._2.toInt}.toArray.sorted

    val knn = new KNN(mat) with CosineDistance
    val journalKnn = new KNN(mat) with SameJournalDistanceMetric{
      val journalIds = allJournalIds
      val subMetric = CosineDistance
    }

    val errors = new ErrorCounts(meshIds)
    val jErrors = new ErrorCounts(meshIds)

    (0 until 100).foreach{rowNum =>
      val truth = mat.getRow(rowNum)
      val pred = mat.getRow(knn.nearest(truth, rowNum))
      errors.addPrediction(pred, truth)
      val jPred = mat.getRow(journalKnn.nearest(truth, rowNum))
      jErrors.addPrediction(jPred, truth)

      if (rowNum % 10 == 0) {
        info("[COSINE: " + errors + "]\t[J-COSINE: " + jErrors + "]")
      }
    }
  }

  //Util functions that should get moved to sblaj
  def expandVector(codeLookup: Array[String], vector: BaseSparseBinaryVector): Map[Int,String] = {
    vector.map{idx => idx -> codeLookup(idx)}.toMap
  }


}

class RunEvaluationArgs extends MatrixLoaderArgs


class MatrixLoaderArgs extends FieldArgs {
  var maxBytes: Long = 1e8.toLong
  var featureSetName: String = "2010_plus"

  lazy val fileSet = trainingIntVectorFileSet(featureSetName).sampleSizeLimitIntVectors(maxBytes)
  lazy val mat = VectorIO.loadMatrix(fileSet)

  lazy val codeLookup = ArrayCodeLookup.loadFromText(mat.nCols, scala.io.Source.fromFile(fileSet.getMergedDictionaryFile))
  lazy val revLookup = {
    val t = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[String]()
    (0 until codeLookup.arr.size).foreach{idx => t.put(codeLookup(idx), idx)}
    t
  }


}