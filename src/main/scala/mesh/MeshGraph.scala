package mesh

import collection._
import bioasq.DataFiles
import io.Source

class MeshGraph(val idToNode: Map[String,MeshNode])

class MeshNode(
  val id: String,
  val children: mutable.Set[MeshNode],
  val parents: mutable.Set[MeshNode]
) {
  override
  def toString() = id
}

object MeshGraph {
  def fromFile(file: String = DataFiles.MeshGraph): MeshGraph = {
    val nodes = mutable.Map[String, MeshNode]()
    Source.fromFile(file).getLines().foreach{ line =>
      val p = line.split("\\s")
      require(p.length == 2)
      val lineNodes = p.map{
        id => nodes.getOrElseUpdate(id, new MeshNode(id, mutable.Set(), mutable.Set()))
      }
      lineNodes(0).children += lineNodes(1)
      lineNodes(1).parents += lineNodes(0)
    }
    new MeshGraph(nodes)
  }
}