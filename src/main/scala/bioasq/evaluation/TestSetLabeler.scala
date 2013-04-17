package bioasq.evaluation

import com.quantifind.sumac.{ArgMain, FieldArgs}
import bioasq.DataFiles
import bioasq.parsers.{AbstractFeaturizer, LabeledAbstractSet, LabeledAbstract, AbstractParser}
import org.sblaj.io.VectorIO
import org.sblaj.featurization.{Murmur64, ArrayCodeLookup}
import bioasq.ml.{CosineDistance, KNN}
import org.sblaj.BaseSparseBinaryVector
import collection.JavaConverters._
import collection._
import org.sblaj.util.Logging

object TestSetLabeler extends ArgMain[TestSetLabelerArgs] with Logging {
  def main(args: TestSetLabelerArgs) {
    info("loading test set")
    val testSet = AbstractParser.parseTestAbstracts(DataFiles.testSetAbstractJson(args.testSet))

    info("Loading knn")
    //label w/ KNN
    val trainingFileSet = DataFiles.trainingIntVectorFileSet(args.featureSetName).sampleSizeLimitIntVectors(args.maxBytes)
    val mat = VectorIO.loadMatrix(trainingFileSet)
    val codeLookup = ArrayCodeLookup.loadFromText(mat.nCols, io.Source.fromFile(trainingFileSet.getMergedDictionaryFile))
    val long2Int = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap()
    (0 until codeLookup.arr.size).foreach{idx => long2Int.put(Murmur64.hash64(codeLookup(idx)), idx)}
    val revLookup = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[String]()
    (0 until codeLookup.arr.size).foreach{idx => revLookup.put(codeLookup(idx), idx)}

    val meshIdMap = revLookup.asScala.filter{case(k,v) => k.startsWith("MESH:")}
    val meshIds = meshIdMap.values.map{_.toInt}.toArray.sorted

    val knn = new KNN(mat) with CosineDistance

    info("labeling test set")
    val labeled = new mutable.ArrayBuffer[LabeledAbstract]()
    val arr = new Array[Int](codeLookup.arr.size) //max possible
    val testVector = new BaseSparseBinaryVector(arr, 0, 0)
    var idx = 0
    testSet.foreach{testAbs =>
      idx += 1
      if (idx % 10 == 0) {
        info("labeling test abstract " + idx)
      }
      //featurize the test data
      val nFeatures = AbstractFeaturizer.testAbtractFeaturize(testAbs, long2Int, arr)
      testVector.reset(arr, 0, nFeatures)

      //"label" it by finding the nearest neighbor
      val nearestIdx = knn.nearest(testVector, -1)
      val nearestVector = mat.getRow(nearestIdx)

      //extract labels from nearest neighbor
      val labels = extractLabels(nearestVector, meshIds, codeLookup.arr)
      labeled += new LabeledAbstract(labels = labels, pmid = testAbs.pmid)
    }

    info("saving labeled test set")
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
        vIdx += 1
        lIdx += 1
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