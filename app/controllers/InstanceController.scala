/*
 * This file is part of the modicio-insights software.
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
import modicio.nativelang.input.NativeDSLParser

import javax.inject.{Inject, Singleton}
import modules.instances.formdata.{NewAssociationForm, UpdateStringValueForm}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class InstanceController @Inject()(cc: ControllerComponents) extends
  AbstractController(cc) with I18nSupport with Logging {


  def index(selection: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getReferences flatMap (references => {
        if (selection.isBlank) {
            Future.successful(Ok(views.html.pages.instance_overview(references.toSeq, "", Seq())))
        } else {
          registry.getAll(selection) map (deepInstances => {
            Ok(views.html.pages.instance_overview(references.toSeq, selection, deepInstances.toSeq))
          })
        }
      })
    })
  }

  def addInstance(selection: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.getType(selection, ModelElement.REFERENCE_IDENTITY) flatMap (reference => {
        reference.get.hasSingleton flatMap (hasSingleton => {
          if(hasSingleton){
            Future.failed(throw new UnsupportedOperationException("This model-element is a singleton-instance and can not be instantiated manually"))
          }else{
            val instanceFactory = RegistryProvider.instanceFactory
            instanceFactory.newInstance(selection) map (instance => {
              Redirect(routes.InstanceController.getInstance(selection, instance.getInstanceId))
            })
          }
        })
      })
    })
  }

  def getInstance(selection: String, instanceId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.get(instanceId) flatMap (instanceOption => {
        instanceOption.get.unfold() flatMap (deepInstance => {
          Future.sequence(
            deepInstance.getDeepAssociations.map(association =>
              registry.get(association.targetInstanceId) flatMap (instance =>
                instance.get.unfold()))) map (associatedInstances => {
            val associationData = deepInstance.getDeepAssociations
            val associationMap = associationData.map(data => (data, associatedInstances.find(_.getInstanceId == data.targetInstanceId).get))
              Ok(views.html.pages.instance_details(selection, deepInstance, associationMap))
          })
        })
      })
    })
  }

  def deleteInstance(selection: String, instanceId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {
      registry.autoRemove(instanceId) map (_ => Redirect(routes.InstanceController.index(selection)))
    })
  }

  def getInstanceNative(selection: String, instanceId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegistryProvider.getRegistry flatMap (registry => {

      RegistryProvider.transformer.get.decomposeInstance(instanceId) map (data => {
        val raw = NativeDSLParser.produceString(data)

        Ok(views.html.pages.instance_native(raw))
      })


    })
  }

  def updateAttribute(selection: String, instanceId: String, attributeName: String): Action[AnyContent] =
    Action.async { implicit request: Request[AnyContent] =>
      UpdateStringValueForm.form.bindFromRequest fold(
        errorForm => {
          Future.successful(Redirect(routes.InstanceController.getInstance(selection, instanceId)))
        },
        data => {
          val attributeValue = data.newValue
          RegistryProvider.getRegistry flatMap (registry => {
            registry.get(instanceId) flatMap (instanceOption => {
              instanceOption.get.unfold() map (deepInstance => {
                deepInstance.assignDeepValue(attributeName, attributeValue)
                Redirect(routes.InstanceController.getInstance(selection, instanceId))
              })
            })
          })
        })
    }

  def addAssociation(selection: String, instanceId: String, relation: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    NewAssociationForm.form.bindFromRequest fold(
      errorForm => {
        Future.successful(Redirect(routes.InstanceController.getInstance(selection, instanceId)))
      },
      data => {
        var associatedInstanceId = data.instanceId
        var associateAsType = data.asType
        if (data.instanceId.contains(":")) {
          val parts = data.instanceId.split(":")
          associateAsType = parts(0)
          associatedInstanceId = parts(1)
        }

        (for {
          registry <- RegistryProvider.getRegistry
          targetInstanceOption <- registry.get(instanceId)
          associatedInstanceOption <- registry.get(associatedInstanceId)
          targetInstance <- targetInstanceOption.get.unfold()
          associatedInstance <- associatedInstanceOption.get.unfold()

        } yield (targetInstance, associatedInstance)).map(res => {
          val (targetInstance, associatedInstance) = res
          targetInstance.associate(associatedInstance, associateAsType, relation)
          Redirect(routes.InstanceController.getInstance(selection, instanceId))
        })
      })
  }

}