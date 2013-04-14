package bioasq.ml

import org.sblaj.{BaseSparseBinaryVector, SparseBinaryRowMatrix}
import org.sblaj.util.Logging

class KNN(
  val matrix: SparseBinaryRowMatrix
) extends Logging {
  self: DistanceMetric =>

  def nearest(input: BaseSparseBinaryVector, sampleTraingIdx: Int): Int = {
    var minDistance = Float.MaxValue
    var minDistanceIdx = -1
    val sampleRow = new BaseSparseBinaryVector(matrix.colIds, 0,0)
    (0 until matrix.nRows).foreach{rowIdx =>
      if (rowIdx != sampleTraingIdx) {
        matrix.getRow(rowIdx, sampleRow)
        val dist = distance(input, sampleRow)
        if (dist < minDistance) {
          minDistance = dist
          minDistanceIdx = rowIdx
        }
      }
    }
    minDistanceIdx
  }
}
