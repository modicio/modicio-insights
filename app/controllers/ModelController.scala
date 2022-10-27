/*
 * This file is part of the codi-insights software.
 * Copyright (C) 2022 Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package controllers

import env.RegistryProvider
import modicio.core.ModelElement
import modicio.core.rules.{AssociationRule, AttributeRule, ConnectionInterface, ParentRelationRule, Slot}
import modicio.core.values.ConcreteValue

import javax.inject.{Inject, Singleton}
import modules.model.formdata.{NewAttributeRuleForm, NewConcreteValueRuleForm, NewExtensionRuleForm, NewFragmentForm, NewLinkRuleForm, StringSelectionForm}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ModelController @Inject()(cc: ControllerComponents) extends
  AbstractController(cc) with I18nSupport with Logging {


  def index: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getReferences map (references => Ok(views.html.pages.model_overview(references.toSeq,
        references.find(_.getTypeName == ModelElement.ROOT_NAME).get.getTimeIdentity)))
    })
  }

  def fragment(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(name, identity) flatMap (typeOption => {
        for{
          typeHandle <- typeOption.get.unfold()
          hasSingleton <- typeHandle.hasSingleton
          hasSingletonRoot <- typeHandle.hasSingletonRoot
          allTypes <- registry.getTypes
          allVariants <- registry.getVariantMap
        } yield {
          Ok(views.html.pages.model_element_details(typeHandle, allTypes.toSeq, allVariants.toSeq.sortBy(_._1._1), hasSingleton, hasSingletonRoot))
        }
      })
    })
  }

  def updateSingleton(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(name, identity) flatMap (typeOption => {
        if(identity != ModelElement.REFERENCE_IDENTITY){
          Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
        }else {
          for {
            typeHandle <- typeOption.get.unfold()
            _ <- typeHandle.updateSingletonRoot()
          } yield {
            Redirect(routes.ModelController.fragment(name, identity))
          }
        }
      })
    })
  }


  def addModelElement(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewFragmentForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.index()))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          val typeFactory = RegistryProvider.typeFactory
          val typeName = data.fragmentName
          val isTemplate = data.fragmentType == "TEMPLATE"
          typeFactory.newType(typeName, ModelElement.REFERENCE_IDENTITY, isTemplate) flatMap (newType =>
          registry.setType(newType) map (_ => Redirect(routes.ModelController.fragment(typeName, ModelElement.REFERENCE_IDENTITY))))
        })
      })
  }

  def addExtensionRule(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewExtensionRuleForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.get.unfold() map (typeHandle => {
              val newRule = ParentRelationRule.create(data.parent, ModelElement.REFERENCE_IDENTITY)
              if (newRule.verify()) {
                typeHandle.applyRule(newRule)
              }
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
      })
  }

  def addAttributeRule(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewAttributeRuleForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.get.unfold() map (typeHandle => {
              val nonEmpty = data.nonEmpty == "true"
              val newRule = AttributeRule.create(data.attributeName, data.datatype, nonEmpty)
              if (newRule.verify()) {
                typeHandle.applyRule(newRule)
              }
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
      })
  }

  def addConcreteValueRule(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewConcreteValueRuleForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.get.unfold() map (typeHandle => {
              val isFinal = data.isFinal == "true"
              val newRule = ConcreteValue.create(data.valueType, data.valueName, (data.content + ":" + isFinal).split(":"))
              if (newRule.verify()) {
                typeHandle.applyRule(newRule)
              }
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
      })
  }

  def addLinkRule(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewLinkRuleForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.getOrElse(throw new Exception()).unfold() map (typeHandle => {
              val connectionInterface = new ConnectionInterface(mutable.Buffer[Slot]())
              val newRule = AssociationRule.create(data.linkName, data.targetName, data.multiplicity, connectionInterface)
              if (newRule.verify()) {
                typeHandle.applyRule(newRule)
              }
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
      })
  }

  def addSlot(name: String, identity: String, ruleId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    StringSelectionForm.form.bindFromRequest fold(
      _ => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.getOrElse(throw new Exception()).unfold() map (typeHandle => {
              val variantTime = data.selection.toLong
              typeHandle.applySlot(ruleId, variantTime)
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
      })
  }

  def removeSlot(name: String, identity: String, ruleId: String, variantTime: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.getOrElse(throw new Exception()).unfold() map (typeHandle => {
              typeHandle.removeSlot(ruleId, variantTime)
              Redirect(routes.ModelController.fragment(name, identity))
            })
          })
        })
  }
  def removeRule(name: String, identity: String, ruleId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(name, identity) flatMap (typeOption => {
        typeOption.get.unfold() map (typeHandle => {
          typeHandle.removeRule(ruleId)
          Redirect(routes.ModelController.fragment(name, identity))
        })
      })
    })
  }

  def incrementVariant(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry =>
      registry.incrementVariant map (_ => Redirect(routes.ModelController.index())))
  }


}