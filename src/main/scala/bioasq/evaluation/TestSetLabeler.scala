package bioasq.evaluation

import com.quantifind.sumac.{ArgMain, FieldArgs}
import bioasq.DataFiles
import bioasq.parsers.{LabeledAbstractSet, LabeledAbstract, AbstractParser}
import org.sblaj.io.VectorIO
import org.sblaj.featurization.ArrayCodeLookup
import bioasq.ml.{CosineDistance, KNN}
import org.sblaj.BaseSparseBinaryVector
import collection.JavaConverters._
import collection._

object TestSetLabeler extends ArgMain[TestSetLabelerArgs] {
  def main(args: TestSetLabelerArgs) {
    val testSet = AbstractParser.parseTestAbstracts(DataFiles.testSetAbstractJson(args.testSet))

    //label w/ KNN
    val trainingFileSet = DataFiles.trainingIntVectorFileSet(args.featureSetName).sampleSizeLimitIntVectors(args.maxBytes)
    val mat = VectorIO.loadMatrix(trainingFileSet)
    val codeLookup = ArrayCodeLookup.loadFromText(mat.nCols, io.Source.fromFile(trainingFileSet.getMergedDictionaryFile))
    val revLookup = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[String]()
    (0 until codeLookup.arr.size).foreach{idx => revLookup.put(codeLookup(idx), idx)}

    val meshIdMap = revLookup.asScala.filter{case(k,v) => k.startsWith("MESH:")}
    val meshIds = meshIdMap.values.map{_.toInt}.toArray.sorted

    val knn = new KNN(mat) with CosineDistance

    val labeled = new mutable.ArrayBuffer[LabeledAbstract]()
    testSet.foreach{testAbs =>
      //featurize the test data
      val testVector = new BaseSparseBinaryVector(null, 0, 0) //TODO

      //"label" it by finding the nearest neighbor
      val nearestIdx = knn.nearest(testVector, -1)
      val nearestVector = mat.getRow(nearestIdx)

      //extract labels from nearest neighbor
      val labels = extractLabels(nearestVector, meshIds, codeLookup.arr)
      labeled += new LabeledAbstract(labels = labels, pmid = testAbs.pmid)
    }

    val outFile = DataFiles.testSetResultJson(args.testSet, args.systemName)
    AbstractParser.writeResults(new LabeledAbstractSet(labeled), outFile)
  }

  def extractLabels(vector: BaseSparseBinaryVector, labels: Array[Int], idMap: Array[String]): mutable.ArrayBuffer[String] = {
    val s = mutable.ArrayBuffer[String]()
    var vIdx = vector.startIdx
    var lIdx = 0
    while (vIdx < vector.endIdx && lIdx < labels.size) {
      if (vector.colIds(vIdx) < labels(lIdx)) {
        vIdx += 1
      } else if (vector.colIds(vIdx) > labels(lIdx)) {
        lIdx += 1
      } else {
        s += idMap(vector.colIds(vIdx))
      }
    }
    s
  }
}

class TestSetLabelerArgs extends FieldArgs {
  var testSet: String = _
  var featureSetName: String = _
  var systemName: String = _
  var maxBytes: Long = 4e9.toLong //4GB
}