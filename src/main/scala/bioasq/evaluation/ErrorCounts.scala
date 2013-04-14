package bioasq.evaluation

import org.sblaj.BaseSparseBinaryVector

/**
 *
 */

class ErrorCounts(
  val meshIds: Array[Int]
) {
  var nSamples = 0
  var totalTruePositives = 0l
  var totalFalsePositives = 0l
  var totalFalseNegatives = 0l
  val meshFalsePositives = new Array[Int](meshIds.length)
  val meshFalseNegatives = new Array[Int](meshIds.length)
  val meshTruthCounts = new Array[Int](meshIds.length)

  val revMeshMapping = {
    val maxId = meshIds.max + 1
    val t = Array.fill(maxId)(-1)
    (0 until meshIds.length).foreach{idx => t(meshIds(idx)) = idx}
    t
  }

  override def toString(): String = {
    "samples = %d; tp = %d; fp = %d; fn = %d".format(nSamples, totalTruePositives, totalFalsePositives, totalFalseNegatives)
  }

  /**
   * prediction & truth can have data in them that isn't meshIds -- those columns will be ignored
   *
   * @param prediction
   * @param truth
   */
  def addPrediction(prediction: BaseSparseBinaryVector, truth: BaseSparseBinaryVector) {
    nSamples += 1
    var predictionIdx = prediction.startIdx
    var truthIdx = truth.startIdx
    var meshIdx = 0
    //3-way parallel walk along the arrays
    while (predictionIdx < prediction.endIdx && truthIdx < truth.endIdx && meshIdx < meshIds.length) {
      //advance prediction & truth to be >= the next meshId
      while (predictionIdx < prediction.endIdx && prediction.colIds(predictionIdx) < meshIds(meshIdx)) {
        predictionIdx += 1
      }
      while (truthIdx < truth.endIdx && truth.colIds(truthIdx) < meshIds(meshIdx)) {
        truthIdx += 1
      }
      if (predictionIdx < prediction.endIdx && truthIdx < truth.endIdx) {
        val (p,c) = (prediction.colIds(predictionIdx), truth.colIds(truthIdx))
        val m = math.min(p,c)
        while (meshIdx < meshIds.length && meshIds(meshIdx) < m){
          meshIdx += 1
        }
        if (meshIdx < meshIds.length && m == meshIds(meshIdx)) {
          //this mesh id appears in either the truth or the prediction
          meshTruthCounts(revMeshMapping(m)) += 1
          if (p > m) {
            //false negative.  this mesh id is in the truth, but not in the prediction
            totalFalseNegatives += 1
            meshFalseNegatives(revMeshMapping(m)) += 1
            truthIdx += 1
          } else if (c > m) {
            //false positive
            totalFalsePositives += 1
            meshFalsePositives(revMeshMapping(m)) += 1
            predictionIdx += 1
          } else {
            //true positive
            totalTruePositives += 1
            predictionIdx += 1
            truthIdx += 1
          }
          meshIdx += 1
        }
      }
    }
    //handle "left-over" at end of one either truth or prediction
    while (predictionIdx < prediction.endIdx && meshIdx < meshIds.length) {
      val (p, t) = (prediction.colIds(predictionIdx), meshIds(meshIdx))
      if (p < t) {
        predictionIdx += 1
      } else if (t < p) {
        meshIdx += 1
      } else {
        //false positive
        totalFalsePositives += 1
        meshFalsePositives(revMeshMapping(p)) += 1
        predictionIdx += 1
        meshIdx += 1
      }
    }
    while (truthIdx < truth.endIdx && meshIdx < meshIds.length) {
      val (c, t) = (truth.colIds(truthIdx), meshIds(meshIdx))
      if (c < t) {
        truthIdx += 1
      } else if (t < c) {
        meshIdx += 1
      } else {
        //false negative
        totalFalseNegatives += 1
        meshFalseNegatives(revMeshMapping(c)) += 1
        truthIdx += 1
        meshIdx += 1
      }
    }
  }
}
