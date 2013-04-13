package bioasq.ml

import org.sblaj.{BaseSparseBinaryVector, SparseBinaryRowMatrix}

class KNN(
  val matrix: SparseBinaryRowMatrix
) {
  self: DistanceMetric =>

  def nearest(input: BaseSparseBinaryVector, sampleTraingIdx: Int): Int = {
    var minDistance = Float.MaxValue
    var minDistanceIdx = -1
    val sampleRow = new BaseSparseBinaryVector(matrix.colIds, 0,0)
    (0 until matrix.nRows).foreach{rowIdx =>
      if (rowIdx != sampleTraingIdx) {
        sampleRow.reset(matrix.colIds, matrix.rowStartIdx(rowIdx), matrix.rowStartIdx(rowIdx + 1))
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
