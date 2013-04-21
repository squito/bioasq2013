package bioasq.ml

import org.sblaj.SparseBinaryRowMatrix
import org.sblaj.featurization.ArrayCodeLookup
import bioasq.parsers.AbstractFeaturizer
import com.quantifind.sumac.ArgMain
import bioasq.evaluation.MatrixLoaderArgs
import org.sblaj.util.Logging
import java.io.{DataOutputStream, FileOutputStream, BufferedOutputStream}
import it.unimi.dsi.fastutil.io.{BinIO, FastByteArrayOutputStream}

class MeshConditionalCounts(
  val mat: SparseBinaryRowMatrix,
  val codes: ArrayCodeLookup[String],
  val k: Int
) extends Logging {

  val colSums = mat.getColSums()
  val meshIds = CodeSet(
    (0 until codes.arr.length).filter{idx => codes.arr(idx).startsWith(AbstractFeaturizer.MESH_PREFIX)}.toArray,
    mat.nCols
  )

  val topMeshIds = MeshUtils.topMeshIds(meshIds, colSums, k)

  def computeMeshConditionalCounts(): Array[Int] = {

    val counts = new Array[Int](k * mat.nCols)
    val meshBuffer = new Array[Int](k)
    (0 until mat.nRows).foreach{row =>
      val vector = mat.getRow(row)
      //first, find the mesh ids that are present in this vector
      var meshIdx = 0
      var nMesh = 0
      var featureIdx = vector.startIdx
      while (meshIdx < k && featureIdx < vector.endIdx) {
        if (topMeshIds.codes(meshIdx) < vector.colIds(featureIdx)) {
          meshIdx += 1
        } else if (topMeshIds.codes(meshIdx) > vector.colIds(featureIdx)) {
          featureIdx += 1
        } else {
          meshBuffer(nMesh) = meshIdx
          nMesh += 1
          meshIdx += 1
          featureIdx += 1
        }
      }

      //now go through the vector again, and increment all mesh counts for each feature
      featureIdx = vector.startIdx
      while (featureIdx < vector.endIdx) {
        var m = 0
        val f = vector.colIds(featureIdx)
        while (m < nMesh) {
          counts(f * k + meshBuffer(m)) += 1
          m += 1
        }
        featureIdx += 1
      }
    }
    counts
  }

  def maxMeshLift(counts: Array[Int]): Array[Double] = {
    //now for each feature, get smoothed lift of that feature for each mesh label, and keep max
    val topMeshCounts = topMeshIds.codes.map{id => colSums(id)}
    val maxLifts = new Array[Double](mat.nCols)
    (0 until mat.nCols).foreach{col =>
      val featureCount = colSums(col)
      var max = 0d
      (0 until k).foreach{ m =>
        val tp = counts(col * k + m)  //this feature w/ the mesh annotation
        val absRR = math.abs(UnivariateTests.binomialRatioConfidence(tp, featureCount, topMeshCounts(m), mat.nRows))
        if (absRR > max)
          max = absRR
      }
      maxLifts(col) = max
    }
    maxLifts
  }

}

object MeshConditionalCounts extends ArgMain[MeshConditionalCountsArgs] with Logging {
  def main(args: MeshConditionalCountsArgs) {

    val helper = new MeshConditionalCounts(args.mat, args.codeLookup, args.k)

    info("mat = " + helper.mat)
    info("colSums = " + helper.colSums.slice(0,100).mkString(","))

    info("starting to compute conditional counts")
    val conditionalCounts = helper.computeMeshConditionalCounts()
    info("finished computing conditional counts")
    val maxLifts = helper.maxMeshLift(conditionalCounts)
    info("finished maxLifts")

    //take a look at a few, just to see
    maxLifts.zipWithIndex.sortBy{-_._1}.take{100}.foreach{case(score,id) =>
      println(helper.codes(id) -> score)
    }

    //reset all the MESH lifts to 0, since they won't be available in the real documents
    helper.meshIds.codes.foreach{idx => maxLifts(idx) = 0}
    //save the lifts out in binary
    writeLifts(maxLifts, args.liftFile)
  }

  def writeLifts(lifts: Array[Double], file: String) {
    BinIO.storeDoubles(lifts, file)
  }

  def readLifts(file:String): Array[Double] = {
    BinIO.loadDoubles(file)
  }

}


class MeshConditionalCountsArgs extends MatrixLoaderArgs {
  var k: Int = 10
  var liftFile: String = "max_mesh_lifts.bin"
}
