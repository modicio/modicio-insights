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

package modules.model.formdata

import play.api.data.Forms._
import play.api.data._

object NewConcreteValueRuleForm {

  case class Data(valueType: String, valueName: String, content: String, isFinal: String)

  val form: Form[Data] = Form(
    mapping(
      "valueType" -> nonEmptyText,
      "valueName" -> nonEmptyText,
      "content" -> nonEmptyText,
      "isFinal" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

}
