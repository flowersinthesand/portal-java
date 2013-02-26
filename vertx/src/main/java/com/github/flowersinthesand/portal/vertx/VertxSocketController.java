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
package com.github.flowersinthesand.portal.vertx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.ServerWebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Init;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.SocketController;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketController")
public class VertxSocketController implements SocketController {

	@Wire
	private String url;
	@Wire
	private HttpServer httpServer;
	@Wire
	private VertxSocketFactory socketFactory;

	@Init
	public void init() {
		RouteMatcher routeMatcher = new RouteMatcher();
		routeMatcher.get("/portal/:file.js", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest req) {
				// TODO enhance 
				URL url = Thread.currentThread().getContextClassLoader().getResource("META-INF/resources" + req.path);
				if (url == null) {
					req.response.statusCode = 404;
					req.response.end();
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					write(url.openStream(), baos);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				req.response
				.putHeader("Content-Type", "application/javascript")
				.putHeader("Content-Length", String.valueOf(baos.size()))
				.end(new Buffer(baos.toByteArray()));
			}
		});
		routeMatcher.get(url, new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest req) {
				req.response.headers().putAll(AbstractSocketFactory.noCacheHeader());
				req.response.headers().putAll(AbstractSocketFactory.corsHeader(req.headers().get("Origin")));
				String when = req.params().get("when");
				if (when.equals("open") || when.equals("poll")) {
					socketFactory.openHttp(req);
				} else if (when.equals("abort")) {
					socketFactory.abort(req.params().get("id"));
				}
			}
		});
		routeMatcher.post(url, new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest req) {
				req.response.headers().putAll(AbstractSocketFactory.noCacheHeader());
				req.response.headers().putAll(AbstractSocketFactory.corsHeader(req.headers().get("Origin")));
				req.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer body) {
						socketFactory.fire(body.toString().substring("data=".length()));
					};
				});
				req.response.end();
			}
		});
		routeMatcher.noMatch(httpServer.requestHandler());
		httpServer.requestHandler(routeMatcher);

		httpServer.websocketHandler(new Handler<ServerWebSocket>() {
			Handler<ServerWebSocket> old = httpServer.websocketHandler();
			@Override
			public void handle(ServerWebSocket webSocket) {
				if (webSocket.path.startsWith(url)) {
					socketFactory.openWs(webSocket);
				}
				if (old != null) {
					old.handle(webSocket);
				}
			}
		});
	}

	private void write(InputStream in, OutputStream out) throws IOException {
		try {
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
		} finally {
			try {
				in.close();
			} catch (IOException ex) {}
			try {
				out.close();
			} catch (IOException ex) {}
		}
	}

}
