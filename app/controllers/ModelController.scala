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
import modicio.codi.Fragment
import modicio.codi.rules.{AssociationRule, AttributeRule, ExtensionRule}
import modicio.codi.values.ConcreteValue

import javax.inject.{Inject, Singleton}
import modules.model.formdata.{NewAttributeRuleForm, NewConcreteValueRuleForm, NewExtensionRuleForm, NewFragmentForm, NewLinkRuleForm}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ModelController @Inject()(cc: ControllerComponents) extends
  AbstractController(cc) with I18nSupport with Logging {


  def index: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getReferences map (references => Ok(views.html.pages.model_overview(references.toSeq)))
    })
  }

  def fragment(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(name, identity) flatMap (typeOption => {
        for{
          typeHandle <- typeOption.get.unfold()
          hasSingleton <- typeHandle.hasSingleton
          hasSingletonRoot <- typeHandle.hasSingletonRoot
        } yield {
          Ok(views.html.pages.fragment_details(typeHandle, hasSingleton, hasSingletonRoot))
        }
      })
    })
  }

  def updateSingleton(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(name, identity) flatMap (typeOption => {
        if(identity != Fragment.REFERENCE_IDENTITY){
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


  def addFragment(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewFragmentForm.form.bindFromRequest fold(
      errorForm => {
        Future.successful(Redirect(routes.ModelController.index()))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          val typeFactory = RegistryProvider.typeFactory
          val typeName = data.fragmentName
          val isTemplate = data.fragmentType == "TEMPLATE"
          val newType = typeFactory.newType(typeName, Fragment.REFERENCE_IDENTITY, isTemplate)
          registry.setType(newType) map (_ => Redirect(routes.ModelController.fragment(typeName, Fragment.REFERENCE_IDENTITY)))
        })
      })
  }

  def addExtensionRule(name: String, identity: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewExtensionRuleForm.form.bindFromRequest fold(
      errorForm => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.get.unfold() map (typeHandle => {
              val newRule = ExtensionRule.create(data.parent, Fragment.REFERENCE_IDENTITY)
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
      errorForm => {
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
      errorForm => {
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
      errorForm => {
        Future.successful(Redirect(routes.ModelController.fragment(name, identity)))
      },
      data => {
        RegistryProvider.getRegistry flatMap (registry => {
          registry.getType(name, identity) flatMap (typeOption => {
            typeOption.getOrElse(throw new Exception()).unfold() map (typeHandle => {
              val newRule = AssociationRule.create(data.linkName, data.targetName, data.multiplicity)
              if (newRule.verify()) {
                typeHandle.applyRule(newRule)
              }
              Redirect(routes.ModelController.fragment(name, identity))
            })
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

}