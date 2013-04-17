package bioasq

import org.sblaj.io.VectorFileSet

object DataFiles {

  private val user = System.getProperty("user.name")
  private val userToProjectRoot = Map(
    "imran" -> "/Users/imran/bioasq2013",
    "peter" -> "/peter/fix/this"
  )

  val DataDir = userToProjectRoot(user) + "/data"
  val MeshDir = DataDir + "/mesh"
  val MeshNames = MeshDir + "/MeSH_name_id_mapping.txt"
  val MeshGraph = MeshDir + "/MeSH_parent_child_mapping.txt"
  val TrainingAbstractsGzip = DataDir + "/training/allMeSH.json.gz"

  val TrainingFeaturizedDir = DataDir + "/training/featurized/"
  val TrainingFeaturizedFileSet = new VectorFileSet(TrainingFeaturizedDir)
  val TrainingIntVectorFileSet = new VectorFileSet(DataDir + "/training/featurized_int")

  def testSetDir(testSetId: String) = DataDir + "/testsets/" + testSetId
  def testSetAbstractJson(testSetId: String) = testSetDir(testSetId) + "/testset.txt"
  def testSetResultJson(testSetId: String, systemId: String) = testSetDir(testSetId) + "/" + systemId + ".json"
}
