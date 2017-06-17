// Your sbt build file. Guides on how to write one can be found at
// http://www.scala-sbt.org/0.13/docs/index.html
val sparkVer = sys.props.getOrElse("spark.version", "2.1.1")
val sparkBranch = sparkVer.substring(0, 3)
val defaultScalaVer = sparkBranch match {
  case "2.0" => "2.11.8"
  case "2.1" => "2.11.8"
  case "2.2" => "2.11.8"
  case _ => throw new IllegalArgumentException(s"Unsupported Spark version: $sparkVer.")
}
val scalaVer = sys.props.getOrElse("scala.version", defaultScalaVer)
val scalaMajorVersion = scalaVer.substring(0, scalaVer.indexOf(".", scalaVer.indexOf(".") + 1))

scalaVersion := "2.11.11"

sparkVersion := "2.1.1"

spName := "databricks/spark-sklearn"

// Don't forget to set the version
version := "0.2.0"

// All Spark Packages need a license
licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

// Add Spark components this package depends on, e.g, "mllib", ....
sparkComponents ++= Seq("mllib")

// uncomment and change the value below to change the directory where your zip artifact will be created
// spDistDirectory := target.value

// add any Spark Package dependencies using spDependencies.
// e.g. spDependencies += "databricks/spark-avro:0.1"

lazy val genClasspath = taskKey[Unit]("Build runnable script with classpath")

genClasspath := {
  import java.io.PrintWriter
  
  val sbtPathRoot = baseDirectory.value / ".sbt.paths"
  sbtPathRoot.mkdirs()

  def writeClasspath(cpType: String)(R: => String): Unit = {
    val fout = new PrintWriter((sbtPathRoot / s"SBT_${cpType}_CLASSPATH").toString)
    println(s"Building ${cpType} classpath for current project")
    try fout.write(R) finally fout.close()
  }

  writeClasspath("RUNTIME") {
    (fullClasspath in Runtime).value.files.map(_.toString).mkString(":")
  }

  writeClasspath("SPARK_PACKAGE") {
    import scala.util.matching.Regex
    val patt = s"(.+?)/(.+?):(.+?)(-s_${scalaMajorVersion})?".r
    val pkgs = spDependencies.value.map { _ match {
      case patt(orgName, pkgName, pkgVer, stem, _*) =>
        if (null != stem) {
          println(s"org ${orgName}, pkg ${pkgName}, ver ${pkgVer}, ${stem}")
          s"${pkgName}-${pkgVer}${stem}.jar"
        } else {
          println(s"org ${orgName}, pkg ${pkgName}, ver ${pkgVer}")
          s"${pkgName}-${pkgVer}.jar"
        }
    }}.toSet
      (fullClasspath in Compile).value.files
      .filter(pkgs contains _.getName())
      .map(_.toString)
      .mkString(":")
  }
}
