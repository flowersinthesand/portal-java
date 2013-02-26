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
    val app = App.find(request.path)
    if (app == null) {
      return null
    }
    
    val controllerClass = classOf[PlaySocketController]
    val socketController = app.bean(controllerClass)
    
    request.method match {
      case "GET" => {
        request.queryString.get("when")(0) match {
          case "open" | "poll" => {
            request.queryString.get("transport")(0) match {
              case "ws" => JavaWebSocket.ofString(socketController.ws)
              case _ => new JavaAction {
                def invocation = socketController.httpOut
                val controller = controllerClass
                lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, "httpOut")
              }
            }
          }
          case "abort" => new JavaAction {
            def invocation = socketController.abort
            val controller = controllerClass
            lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, "abort")
          }
        }
      }
      case "POST" => new JavaAction {
        def invocation = socketController.httpIn
        val controller = controllerClass
        lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, "httpIn")
      }
    }
  }
  
}
