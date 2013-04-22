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
import java.io.File


/**
 *
 */

object RunEvaluation extends ArgMain[RunEvaluationArgs] with Logging {
  def main(args: RunEvaluationArgs) {
    val fileSet = trainingIntVectorFileSet(args.featureSetName).sampleSizeLimitIntVectors(args.maxBytes)
    val mat = VectorIO.loadMatrix(fileSet)
    info("matrix: " + mat)

    val codeLookup = ArrayCodeLookup.loadFromText(mat.nCols, scala.io.Source.fromFile(fileSet.getMergedDictionaryFile))
    val revLookup = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[String]()
    (0 until codeLookup.arr.size).foreach{idx => revLookup.put(codeLookup(idx), idx)}

    val meshIdMap = revLookup.asScala.filter{case(k,v) => k.startsWith(AbstractFeaturizer.MESH_PREFIX)}
    val meshIds = meshIdMap.map{_._2.toInt}.toArray.sorted

    val cosKnn = new KNN(mat) with CosineDistance
    val eucKnn = new KNN(mat) with EuclideanDistance
    val distanceWeights = MeshConditionalCounts.readLifts(args.weightFile)
    val wEucKnn = new KNN(mat) with WeightedEuclideanDistance{
      val weights = distanceWeights
    }

    val cosErrors = new ErrorCounts(meshIds)
    val eucErrors = new ErrorCounts(meshIds)
    val wEucErrors = new ErrorCounts(meshIds)

    args.evalIds.foreach{evalRowStart =>
      val s = if (evalRowStart < 0) mat.nRows + evalRowStart else evalRowStart
      info("beginning eval block at row " + s)
      (s until s + args.evalBlockSize).foreach{ rowNum =>
        val truth = mat.getRow(rowNum)
        val cosPred = mat.getRow(cosKnn.nearest(truth, rowNum))
        cosErrors.addPrediction(cosPred, truth)
        val eucPred = mat.getRow(eucKnn.nearest(truth, rowNum))
        eucErrors.addPrediction(eucPred, truth)
        val wEucPred = mat.getRow(wEucKnn.nearest(truth, rowNum))
        wEucErrors.addPrediction(wEucPred, truth)

        if (cosErrors.nSamples % 10 == 0) {
          info("%80s%80s%80s".format("[COSINE: " + cosErrors + "]", "[EUC: " + eucErrors + "]", "[W-EUC: " + wEucErrors+ "]"))
        }
      }
    }
  }

  //Util functions that should get moved to sblaj
  def expandVector(codeLookup: Array[String], vector: BaseSparseBinaryVector): Map[Int,String] = {
    vector.map{idx => idx -> codeLookup(idx)}.toMap
  }


}

class RunEvaluationArgs extends MatrixLoaderArgs {
  var weightFile = "max_mesh_lifts.bin"
  var evalIds = List(0,593, 1345,5791,34931,134807, -5000, -200)
  var evalBlockSize = 10
}


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