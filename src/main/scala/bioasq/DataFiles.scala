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

  def trainingFeaturizedDir(featurizeName: String) = DataDir + "/training/featurized/" + featurizeName +"/raw"
  def trainingFeaturizedFileSet(featurizeName: String) = new VectorFileSet(trainingFeaturizedDir(featurizeName))
  def trainingIntVectorFileSet(featurizeName: String) = new VectorFileSet(DataDir + "/training/featurized/" + featurizeName + "/int")

  def testSetDir(testSetId: String) = DataDir + "/testsets/" + testSetId
  def testSetAbstractJsonNoJournal(testSetId: String) = testSetDir(testSetId) + "/testset.txt"
  def testSetAbstractJson(testSetId: String) = testSetDir(testSetId) + "/testsetWithJournal.txt"
  def testSetResultJson(testSetId: String, systemId: String) = testSetDir(testSetId) + "/" + systemId + ".json"
}
