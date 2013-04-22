package bioasq.ml

//TODO move to sblaj
class CodeSet(
  val codes: Array[Int],
  val idToIdx: Array[Int] //from the original, global feature id, to its index in this reduced space
) {
  override def toString() = codes.mkString(",")
}

object CodeSet {
  def apply(codes: Array[Int], maxCode: Int) = {
    val reverseLookup = new Array[Int](maxCode)
    (0 until codes.length).foreach{ idx =>
      reverseLookup(codes(idx)) = idx
    }
    new CodeSet(codes, reverseLookup)
  }
}
