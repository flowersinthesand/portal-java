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
package com.github.flowersinthesand.portal.samples.chat;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.deploy.Verticle;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.vertx.VertxModule;

public class ChatServer extends Verticle {

	@Override
	public void start() throws Exception {
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.requestHandler(new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest req) {
				if (req.path.equals("/")) {
					req.response.sendFile("src/main/webapp/index.html");
				}
			}
		});
		
		new App(new Options().url("/chat").packageOf(this), new VertxModule(httpServer));
		httpServer.listen(8080);
	}

}