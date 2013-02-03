/*
 * Copyright 2012-2013 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.flowersinthesand.portal.play

import org.apache.commons.lang3.reflect.MethodUtils

import play.api.mvc.Handler
import play.core._
import play.core.j._
import play.mvc.Http.RequestHeader

import com.github.flowersinthesand.portal.App

object Handlers {
  
  def get(request: RequestHeader): Handler = {
    Option(App.find(request.path)) match {
      case Some(name) => {
        request.method match {
          case "GET" => {
            request.queryString.get("transport")(0) match {
              case "ws" => JavaWebSocket.ofString(Accessor.ws)
              case _ => new JavaAction {
                def invocation = Accessor.httpOut
                lazy val controller = classOf[Accessor]
                lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, "httpOut")
              }
            }
          }
          case "POST" => new JavaAction {
            def invocation = Accessor.httpIn
            lazy val controller = classOf[Accessor]
            lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, "httpIn")
          }
        }
      }
      case None => null
    }
  }

}
