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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.api.mvc.Codec;
import play.core.j.JavaResults;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Results.Chunks;
import play.mvc.WebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.SocketFactory;
import com.github.flowersinthesand.portal.support.AbstractSocket;
import com.github.flowersinthesand.portal.support.ReplyHandler;

@Bean("socketFactory")
public class PlaySocketFactory implements SocketFactory {

	private final Logger logger = LoggerFactory.getLogger(PlaySocketFactory.class);
	private Map<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	@Wire
	private Dispatcher dispatcher;
	@Wire
	private ReplyHandler replyHandler;

	@Override
	public Socket find(String id) {
		return sockets.get(id);
	}

	public WebSocket<String> openWsSocket(Request request) {
		WsSocket socket = new WsSocket(request);
		sockets.put(socket.id(), socket);

		return socket.webSocket;
	}

	public Chunks<String> openHttpSocket(Request request, Response response) {
		String id = request.queryString().get("id")[0];
		String transport = request.queryString().get("transport")[0];

		HttpSocket socket = null;
		if (transport.equals("sse") || transport.startsWith("stream")) {
			socket = new StreamSocket(request, response);
			sockets.put(id, socket);
		} else if (transport.startsWith("longpoll")) {
			if ("1".equals(request.queryString().get("count")[0])) {
				socket = new LongPollSocket(request, response);
				sockets.put(id, socket);
			} else {
				socket = (LongPollSocket) sockets.get(id);
				((LongPollSocket) socket).refresh(request, response, false);
			}
		}

		return socket.chunks;
	}

	public void fire(String raw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> m = Json.fromJson(Json.parse(raw), Map.class);
		logger.info("Receiving an event {}", m);
		dispatcher.fire((String) m.get("type"), sockets.get(m.get("socket")), m.get("data"), (Boolean) m.get("reply") ? (Integer) m.get("id") : 0);
	}
	
	abstract class PlaySocket extends AbstractSocket {

		@Override
		public boolean opened() {
			return sockets.containsValue(this);
		}

		@Override
		protected void bindReply(Fn.Callback callback) {
			replyHandler.set(id(), eventId.get(), callback);
		}

		@Override
		protected void bindReply(Fn.Callback1<?> callback) {
			replyHandler.set(id(), eventId.get(), callback);
		}

		protected void onOpen() {
			logger.info("Socket#{} has been opened, params: {}", id(), params);
			dispatcher.fire("open", this);
		}

		protected void onClose() {
			logger.info("Socket#{} has been closed", id());
			dispatcher.fire("close", sockets.remove(id()));
		}

	}

	class WsSocket extends PlaySocket {

		private WebSocket<String> webSocket;
		private WebSocket.Out<String> out;

		public WsSocket(Request request) {
			this.params = params(request.queryString());
			this.webSocket = new WebSocket<String>() {
				@Override
				public void onReady(WebSocket.In<String> in, Out<String> oout) {
					out = oout;
					in.onClose(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});
					in.onMessage(new F.Callback<String>() {
						@Override
						public void invoke(String message) throws Throwable {
							fire(message);
						}
					});
					onOpen();
				}
			};
		}

		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}

	}
	
	abstract class HttpSocket extends PlaySocket {

		protected Chunks<String> chunks;
		protected Chunks.Out<String> out;
		
		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}
		
	}

	class StreamSocket extends HttpSocket {

		public StreamSocket(Request request, Response response) {
			this.params = params(request.queryString());
			this.isAndroid = isAndroid(request.getHeader("user-agent"));
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});

					out.write(padding2K);
					if (isAndroid) {
						out.write(padding2K);
					}
					out.write("\n");
					onOpen();
				}
			};
			response.setContentType(("text/" + ("sse".equals(param("transport")) ? "event-stream" : "plain") + "; charset=utf-8"));
		}

	}

	class LongPollSocket extends HttpSocket {
		
		private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();
		
		public LongPollSocket(Request request, Response response) {
			this.params = params(request.queryString());
			refresh(request, response, true);
		}

		private void refresh(final Request request, Response response, final boolean first) {
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							if (!first && out != null) {
								onClose();
							}
						}
					}); 
					
					if (first) {
						out.close();
						onOpen();
					} else {
						// TODO enhance
						Integer lastEventId = Integer.valueOf(request.queryString().get("lastEventId")[0]);
						List<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
						for (Map<String, Object> message : cache) {
							if (lastEventId < (Integer) message.get("id")) {
								temp.add(message);
							}
						}

						if (!temp.isEmpty()) {
							logger.debug("With the last event id {}, flushing cached messages {}", lastEventId, temp);
							transmit(format(temp));
							cache.clear();
						}
					}
				}
			};
			response.setContentType(("text/" + ("longpolljsonp".equals(param("transport")) ? "javascript" : "plain") + "; charset=utf-8"));
		}

		@Override
		protected void cache(Map<String, Object> message) {
			cache.add(message);
		}

		@Override
		protected void transmit(String it) {
			super.transmit(it);
			out.close();
		}

	}

}
