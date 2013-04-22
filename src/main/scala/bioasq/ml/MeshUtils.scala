package bioasq.ml

object MeshUtils {

  def topMeshIds(meshIds: CodeSet, colSums: Array[Int], k: Int): CodeSet = {
    val topIds = meshIds.codes.map{id => id -> colSums(id)}.sortBy{-_._2}.take(k)
    CodeSet(topIds.map{_._1}, meshIds.idToIdx.length)
  }
}
