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

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketFactory")
public class VertxSocketFactory extends AbstractSocketFactory {

	void openWs(ServerWebSocket webSocket) {
		WsSocket socket = new WsSocket(webSocket);
		sockets.put(socket.id(), socket);
		socket.onOpen();
	}

	void openHttp(HttpServerRequest req) {
		String when = req.params().get("when");
		String id = req.params().get("id");
		String transport = req.params().get("transport");

		if (transport.equals("sse") || transport.startsWith("stream")) {
			StreamSocket socket = new StreamSocket(req);
			sockets.put(id, socket);
			socket.onOpen();
		} else if (transport.startsWith("longpoll")) {
			if (when.equals("open")) {
				LongPollSocket socket = new LongPollSocket(req);
				sockets.put(id, socket);
				socket.onOpen();
			} else if (when.equals("poll")) {
				((LongPollSocket) sockets.get(id)).refresh(req, false);
			}
		}
	}

	class WsSocket extends AbstractSocket {

		private ServerWebSocket webSocket;

		public WsSocket(ServerWebSocket webSocket) {
			this.webSocket = webSocket;
			webSocket.query();
			Map<String, List<String>> paramMap = new QueryStringDecoder("?" + webSocket.query()).parameters();
			params = new LinkedHashMap<>(paramMap.size());
			for (Map.Entry<String, List<String>> entry : paramMap.entrySet()) {
				params.put(entry.getKey(), entry.getValue().get(0));
			}
			
			webSocket.dataHandler(new Handler<Buffer>() {
				@Override
				public void handle(Buffer data) {
					fire(data.toString());
				}
			});
			webSocket.exceptionHandler(new Handler<Throwable>() {
				@Override
				public void handle(Throwable event) {
					onClose();
				}
			});
			webSocket.closeHandler(new VoidHandler() {
				@Override
				protected void handle() {
					onClose();
				}
			});
		}

		@Override
		protected void transmit(String it) {
			webSocket.writeTextFrame(it);
		}

		@Override
		protected void disconnect() {
			webSocket.close();
		}

	}

	class StreamSocket extends AbstractSocket {

		private HttpServerResponse res;

		public StreamSocket(HttpServerRequest req) {
			this.res = req.response();
			this.params = new LinkedHashMap<>();
			for (Entry<String, String> entry : req.params()) {
				this.params.put(entry.getKey(), entry.getValue());
			}
			this.isAndroid = isAndroid(req.headers().get("user-agent"));
			res.exceptionHandler(new Handler<Throwable>() {
				@Override
				public void handle(Throwable event) {
					onClose();
				}
			});
			res.closeHandler(new VoidHandler() {
				@Override
				protected void handle() {
					onClose();
				}
			});
			res.setChunked(true).putHeader("content-type", streamContentType() + "; charset=utf-8");
			res.write(padding2K);
			if (isAndroid) {
				res.write(padding2K);
			}
			res.write("\n");
		}

		@Override
		protected void transmit(String it) {
			res.write(it);
		}

		@Override
		protected void disconnect() {
			res.close();
		}

	}

	class LongPollSocket extends AbstractSocket {

		private HttpServerResponse res;

		public LongPollSocket(HttpServerRequest req) {
			this.params = new LinkedHashMap<>();
			for (Entry<String, String> entry : req.params()) {
				this.params.put(entry.getKey(), entry.getValue());
			}
			refresh(req, true);
		}

		private void refresh(HttpServerRequest req, final boolean open) {
			res = req.response();
			res.exceptionHandler(new Handler<Throwable>() {
				@Override
				public void handle(Throwable event) {
					onClose();
				}
			});
			res.closeHandler(new VoidHandler() {
				@Override
				protected void handle() {
					if (!open && res != null) {
						onClose();
					}
				}
			});
			res.putHeader("content-type", longpollContentType() + "; charset=utf-8");
			if (open) {
				res.end();
				res.close();
			} else {
				retrieveCache(req.params().get("lastEventIds"));
			}
		}

		@Override
		protected void transmit(String it) {
			if (res != null) {
				HttpServerResponse response = res;
				res = null;
				response.end(it);
				response.close();
			}
		}

		@Override
		protected void disconnect() {
			if (res != null) {
				res.close();
			}
		}

	}

}