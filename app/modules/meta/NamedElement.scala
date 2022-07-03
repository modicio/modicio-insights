package modules.meta

import codi.core.rules.{AssociationRule, AttributeRule, BehaviourRule, ConstraintRule}
import codi.core.{BaseModel, Fragment}

import scala.concurrent.Future

class NamedElement extends BaseModel(name = "NamedElement", identity = Fragment.REFERENCE_IDENTITY, isTemplate = true){

  override def onModelChange(): Future[Unit] = {
    Future.successful()
  }

  override def getAttributeRules: Set[AttributeRule] = {
    Set(
      AttributeRule.create(name = "Title", datatype = "String", nonEmpty = true)
    )
  }

  override def getAssociationRules: Set[AssociationRule] = Set()

  override def getConstraintRules: Set[ConstraintRule] = Set()

  override def getBehaviourRules: Set[BehaviourRule] = Set()

}
