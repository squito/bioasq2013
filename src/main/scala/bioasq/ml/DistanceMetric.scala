package bioasq.ml

import org.sblaj.BaseSparseBinaryVector

trait DistanceMetric {
  def distance(row1: BaseSparseBinaryVector, row2: BaseSparseBinaryVector): Float
}

trait CosineDistance extends DistanceMetric {
  def distance(row1: BaseSparseBinaryVector, row2: BaseSparseBinaryVector): Float = {
    (row1.dot(row2).toFloat / (math.sqrt(row1.nnz) * math.sqrt(row2.nnz))).toFloat
  }
}

trait EuclideanDistance extends DistanceMetric {
  def distance(row1: BaseSparseBinaryVector, row2: BaseSparseBinaryVector): Float = {
    var idx1 = row1.startIdx
    var idx2 = row2.startIdx
    var sum = 0
    while (idx1 < row1.endIdx && idx2 < row2.endIdx) {
      if (row1.colIds(idx1) == row2.colIds(idx2)) {
        //both agree, add 0 to distance
        idx1 += 1
        idx2 += 1
      } else if (row1.colIds(idx1) < row2.colIds(idx2)) {
        idx1 += 1
        sum += 1
      } else {
        idx2 += 1
        sum += 1
      }
    }
    math.sqrt(sum).toFloat
  }
}

/**
 * lets say the distance between two abstracts is infinite if their not in the same journal
 */
trait SameJournalDistanceMetric extends DistanceMetric {
  val journalIds: Array[Int]  //must be sorted
  val subMetric: DistanceMetric

  def distance(row1: BaseSparseBinaryVector, row2:BaseSparseBinaryVector): Float = {
    //there should only be one journal, so find it in row1 ...
    var idx = 0
    var jIdx = 0
    var keepGoing = true
    while (keepGoing && idx < row1.startIdx && jIdx < journalIds.length) {
      if (row1.colIds(idx) == journalIds(jIdx)) {
        keepGoing = false
      } else if (row1.colIds(idx) < journalIds(jIdx)) {
        idx += 1
      } else {
        jIdx += 1
      }
    }
    val journal = row1.colIds(idx)
    if (row2.get(journal) == 1) {
      subMetric.distance(row1,row2)
    } else {
      Float.MaxValue
    }
  }
}