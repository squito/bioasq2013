package mesh

import org.scalatest.FunSuite

class MeshGraphTest extends FunSuite {

  test("graph parsing") {
    val graph = MeshGraph.fromFile()
    println("nodes = " + graph.idToNode.size)
    println("roots = " + graph.idToNode.values.filter{_.parents.isEmpty}.size)
    println("leaves = " + graph.idToNode.values.filter{_.children.isEmpty}.size)
    println("avg # parents = " + graph.idToNode.values.map{_.parents.size}.sum.toDouble / graph.idToNode.size)
    println("avg # children = " + graph.idToNode.values.map{_.children.size}.sum.toDouble / graph.idToNode.size)
    println("num edges = " + graph.idToNode.values.map{_.children.size}.sum)

    //only one root, so lets see what it is:
    val roots = graph.idToNode.values.filter{_.parents.isEmpty}.toSet
    println("root = " + roots)
    //lets see how many labels only have the root as their parent
    val level1 = graph.idToNode.values.filter{node => node.parents.size == 1 && node.parents.diff(roots).isEmpty}
    println("level 1 = " + level1.size + "\t:\t" + level1)
  }
}
