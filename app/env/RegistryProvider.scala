package env

import codi.core.{InstanceFactory, Registry, TypeFactory}
import codi.core.rules.Rule
import codi.nativelang.input.{NativeInput, NativeInputParser, NativeInputTransformator}
import codi.nativelang.logic.{SimpleDefinitionVerifier, SimpleMapRegistry, SimpleModelVerifier}
import modules.meta.NamedElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object RegistryProvider {

  val modelVerifier = new SimpleModelVerifier()
  val definitionVerifier = new SimpleDefinitionVerifier()

  val typeFactory = new TypeFactory(definitionVerifier, modelVerifier)
  val instanceFactory = new InstanceFactory(definitionVerifier, modelVerifier)

  private var registry: Option[Registry] = None

  private def init(): Future[Unit] = {

    //Rules generate their own UUIDs:ss
    Rule.enableAutoID()

    registry = Some(new SimpleMapRegistry(typeFactory, instanceFactory))

    typeFactory.setRegistry(registry.get)
    instanceFactory.setRegistry(registry.get)

    val source = Source.fromFile("resources/json_hybrid_rule_example.json")
    val fileContents = source.getLines.mkString
    println(fileContents)
    source.close()

    val initialInput: NativeInput = NativeInputParser.parse(fileContents)
    val transformator: NativeInputTransformator = new NativeInputTransformator(registry.get, definitionVerifier, modelVerifier)

    for{
      _ <- new NamedElement().setup(registry.get, definitionVerifier)
      _ <- transformator.extendModel(initialInput)
    } yield Future.successful()
  }

  def getRegistry: Future[Registry] = {
    if(registry.isDefined){
      Future.successful(registry.get)
    }else{
      init() map (_ => registry.get)
    }

  }

}
