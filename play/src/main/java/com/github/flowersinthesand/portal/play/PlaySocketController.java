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
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.WebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.SocketController;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketController")
public class PlaySocketController extends Controller implements SocketController {

	@Wire
	private PlaySocketFactory socketFactory;

	public WebSocket<String> ws() {
		return socketFactory.openWs(request());
	}

	public Result httpOut() {
		setHeaders(request(), response());
		return ok(socketFactory.openHttp(request(), response()));
	}

	public Result httpIn() {
		setHeaders(request(), response());
		socketFactory.fire(request().body().asText().substring("data=".length()));
		return ok();
	}

	public Result abort() {
		socketFactory.abort(request().queryString().get("id")[0]);
		return ok();
	}

	private void setHeaders(Request req, Response res) {
		res.getHeaders().putAll(AbstractSocketFactory.noCacheHeader());
		res.getHeaders().putAll(AbstractSocketFactory.corsHeader(req.getHeader("Origin")));
	}

}