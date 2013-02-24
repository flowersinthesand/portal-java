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
package com.github.flowersinthesand.portal.play;

import play.mvc.Controller;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.WebSocket;

import com.github.flowersinthesand.portal.App;

public class Accessor extends Controller {

	public static WebSocket<String> ws() {
		return factory(request().path()).openWs(request());
	}

	public static Result httpOut() {
		setHeaders(response());
		return ok(factory(request().path()).openHttp(request(), response()));
	}

	public static Result httpIn() {
		setHeaders(response());
		factory(request().path()).fire(request().body().asText().substring("data=".length()));
		return ok();
	}
	
	public static Result abort() {
		factory(request().path()).abort(request().queryString().get("id")[0]);
		return ok();
	}
	
	private static PlaySocketFactory factory(String path) {
		return App.find(path).bean(PlaySocketFactory.class);
	}

	private static void setHeaders(Response response) {
		response.setHeader("Expires", "-1");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		
		response.setHeader("Access-Control-Allow-Origin", request().getHeader("Origin") != null ? request().getHeader("Origin") : "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
	}

}