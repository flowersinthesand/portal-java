import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "portal-chat-play"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "com.github.flowersinthesand" % "portal-core" % "0.5-SNAPSHOT",
        "com.github.flowersinthesand" % "portal-javascript" % "0.5-SNAPSHOT",
        "com.github.flowersinthesand" % "portal-play" % "0.5-SNAPSHOT",
        "org.reflections" % "reflections" % "0.9.8"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
        resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
    )

}
