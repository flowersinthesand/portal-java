import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    def fromEnv(name: String) = System.getenv(name) match {
        case null => None
        case value => Some(value)
    }

    val appName         = fromEnv("project.artifactId").getOrElse("portal-chat-play")
    val appVersion      = fromEnv("project.version").getOrElse("1.0-SNAPSHOT")

    val appDependencies = Seq(
        javaCore
        // Dependencies are managed by maven
        // To run the application in play console, uncomment the followings and replace ${portal.version} with the latest version  
        // "com.github.flowersinthesand" % "portal-core" % "${portal.version}",
        // "com.github.flowersinthesand" % "portal-play" % "${portal.version}"
    )

    // play2-maven-plugin 1.2.2 can't handle new dist structure of Play 2.2.0
    // workaround by @maccamlc
    // https://github.com/nanoko-project/maven-play2-plugin/issues/15#issuecomment-24977753
    val distFolder = new File("target/dist")
    distFolder.mkdirs()

    val main = play.Project(appName, appVersion, appDependencies).settings(
        target in com.typesafe.sbt.SbtNativePackager.Universal := distFolder
    )

}
