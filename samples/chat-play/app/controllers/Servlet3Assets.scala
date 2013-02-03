// Logic borrowed from https://github.com/webjars/webjars-play/blob/master/src/main/scala/controllers/WebJarAssets.scala by James Ward
package controllers

import play.api.mvc.{Action, AnyContent}
import play.api.Play
import play.api.Play.current

import java.io.File
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.asScalaSet
import org.reflections.Reflections
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}
import org.reflections.scanners.ResourcesScanner

object Servlet3Assets {
  
  val PATH_PREFIX = List("META-INF", "resources")

  // returns the contents of an asset
  def at(file: String): Action[AnyContent] = {
    Assets.at("/" + PATH_PREFIX.mkString("/"), file)
  }
  
  // this resolves a full path to an asset based on a file suffix
  // todo: cache this
  def locate(file: String): String = {
    val config = new ConfigurationBuilder()
                        .addUrls(ClasspathHelper.forPackage(PATH_PREFIX.mkString("."), Play.application.classloader))
                        .setScanners(new ResourcesScanner())
    
    val reflections = new Reflections(config)
    
    // the map in the reflection store is just the file name so if the file being located doesn't contain a "/" then
    // a shortcut can be taken.  Otherwise the collection of multimap's values need to be searched.
    // Either way the first match is returned (if there is a match)
    if (file.contains("/")) {
      reflections.getStore.getStoreMap.values.map(_.values.find(_.endsWith(file))).head.get.stripPrefix(PATH_PREFIX.mkString("/") + "/")
    }
    else {
      reflections.getStore.getResources(file).head.stripPrefix(PATH_PREFIX.mkString("/") + "/")
    }
  }

}
