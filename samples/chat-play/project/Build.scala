import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    def fromEnv(name: String) = System.getenv(name) match {
        case null => None
        case value => Some(value)
    }

    val appName         = fromEnv("project.artifactId").getOrElse("portal-chat-play")
    val appVersion      = fromEnv("project.version").getOrElse("1.0-SNAPSHOT")

    val appDependencies = Seq(
        // Dependencies are managed by maven
        // To run the application in play console, uncomment the followings and replace ${portal.version} with the latest version  
        // "com.github.flowersinthesand" % "portal-core" % "${portal.version}",
        // "com.github.flowersinthesand" % "portal-play" % "${portal.version}",
        // "org.reflections" % "reflections" % "0.9.8"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings()

}
