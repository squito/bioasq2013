package bioasq

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
}
