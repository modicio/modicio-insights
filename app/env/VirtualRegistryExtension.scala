package env

import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

object VirtualRegistryExtension {

  private val namedVariants = new ConcurrentHashMap[String, String]()

  private val namedVersions = new ConcurrentHashMap[String, String]()

  def getVariantNames: Map[String, String] = namedVariants.asScala.toMap

  def addVariantName(variantId: String, name: String): Unit = namedVariants.put(variantId, name)

  def getVersionNames: Map[String, String] = namedVariants.asScala.toMap

  def addVersionName(runningId: String, name: String): Unit = namedVersions.put(runningId, name)

}
