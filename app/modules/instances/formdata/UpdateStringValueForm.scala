package modules.instances.formdata

import play.api.data.Forms._
import play.api.data._

object UpdateStringValueForm {

  case class Data(newValue: String)

  val form: Form[Data] = Form(
    mapping(
      "newValue" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

}
