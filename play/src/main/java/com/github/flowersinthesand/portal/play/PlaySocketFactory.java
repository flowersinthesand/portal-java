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

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
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
import com.github.flowersinthesand.portal.support.ReplyHandler;

@Bean("socketFactory")
public class PlaySocketFactory implements SocketFactory {

	private static final String padding2K = CharBuffer.allocate(2048).toString().replace('\0', ' ');

	private final Logger logger = LoggerFactory.getLogger(PlaySocketFactory.class);
	private ObjectMapper mapper = new ObjectMapper();
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
		WsSocket socket = new WsSocket(params(request.queryString()));
		sockets.put(socket.id(), socket);

		return socket.webSocket;
	}

	public Chunks<String> openHttpSocket(Request request, Response response) {
		Map<String, String> params = params(request.queryString());
		String id = params.get("id");
		String transport = params.get("transport");

		HttpSocket socket = null;
		if (transport.equals("sse") || transport.startsWith("stream")) {
			socket = new HttpStreamSocket(request, response, params);
			sockets.put(id, socket);
		} else if (transport.startsWith("longpoll")) {
			if ("1".equals(params.get("count"))) {
				socket = new HttpLongPollSocket(request, response, params);
				sockets.put(id, socket);
			} else {
				socket = (HttpLongPollSocket) sockets.get(id);
				((HttpLongPollSocket) socket).refresh(request, response, false);
			}
		}

		return socket.chunks;
	}
	
	private Map<String, String> params(Map<String, String[]> params) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Entry<String, String[]> entry : params.entrySet()) {
			map.put(entry.getKey(), entry.getValue()[0]);
		}

		return map;
	}

	public void fire(String raw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> m = Json.fromJson(Json.parse(raw), Map.class);
		logger.info("Receiving an event {}", m);
		dispatcher.fire((String) m.get("type"), sockets.get(m.get("socket")), m.get("data"), (Boolean) m.get("reply") ? (Integer) m.get("id") : 0);
	}

	abstract class AbstractSocket implements Socket {

		protected Map<String, String> params;
		private AtomicInteger eventId = new AtomicInteger();

		@Override
		public String id() {
			return params.get("id");
		}

		@Override
		public boolean opened() {
			return sockets.containsValue(this);
		}

		@Override
		public String param(String key) {
			return params.get(key);
		}

		@Override
		public Socket send(String event) {
			doSend(event, null, false);
			return this;
		}

		@Override
		public Socket send(String event, Object data) {
			doSend(event, data, false);
			return this;
		}

		@Override
		public Socket send(String event, Object data, Fn.Callback callback) {
			doSend(event, data, true);
			replyHandler.set(id(), eventId.get(), callback);
			return this;
		}

		@Override
		public Socket send(String event, Object data, Fn.Callback1<?> callback) {
			doSend(event, data, true);
			replyHandler.set(id(), eventId.get(), callback);
			return this;
		}

		private void doSend(String type, Object data, boolean reply) {
			Map<String, Object> message = new LinkedHashMap<String, Object>();

			message.put("id", eventId.incrementAndGet());
			message.put("type", type);
			message.put("data", data);
			message.put("reply", reply);

			logger.info("Socket#{} is sending an event {}", id(), message);
			cache(message);
			write(Json.stringify(Json.toJson(message)));
		}

		protected void cache(Map<String, Object> message) {}

		@Override
		public Socket close() {
			logger.info("Closing socket#{}", id());
			disconnect();
			return this;
		}

		protected void onOpen() {
			logger.info("Socket#{} has been opened, params: {}", id(), params);
			dispatcher.fire("open", this);
		}

		protected void onClose() {
			logger.info("Socket#{} has been closed", id());
			dispatcher.fire("close", sockets.remove(id()));
		}

		abstract protected void write(String it);

		abstract protected void disconnect();

	}

	class WsSocket extends AbstractSocket implements Socket {

		private WebSocket<String> webSocket;
		private WebSocket.Out<String> out;

		public WsSocket(Map<String, String> paramMap) {
			params = paramMap;
			webSocket = new WebSocket<String>() {
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
		protected void write(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}

	}
	
	abstract class HttpSocket extends AbstractSocket implements Socket {

		protected Chunks<String> chunks;
		protected Chunks.Out<String> out;

		@Override
		protected void disconnect() {
			out.close();
		}
		
	}

	class HttpStreamSocket extends HttpSocket implements Socket {
		
		private String userAgent;

		public HttpStreamSocket(Request request, Response response, Map<String, String> paramMap) {
			params = paramMap;
			userAgent = request.getHeader("user-agent");
			chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					oout.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});

					oout.write(padding2K);
					if (userAgent.matches(".*Android\\s[23]\\..*")) {
						oout.write(padding2K);
					}
					oout.write("\n");
					onOpen();
				}
			};
			response.setContentType(("text/" + ("sse".equals(param("transport")) ? "event-stream" : "plain") + "; charset=utf-8"));
		}

		@Override
		protected void write(String it) {
			if (userAgent.matches(".*Android\\s[23]\\..*")) {
				out.write(padding2K);
				out.write(padding2K);
			}
			for (String datum : it.split("\r\n|\r|\n")) {
				out.write("data: ");
				out.write(datum);
				out.write("\n");
			}
			out.write("\n");
		}

	}

	class HttpLongPollSocket extends HttpSocket implements Socket {
		
		private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();
		
		public HttpLongPollSocket(Request request, Response response, Map<String, String> paramMap) {
			params = paramMap;
			refresh(request, response, true);
		}

		private void refresh(final Request request, Response response, final boolean first) {
			chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					oout.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							if (!first && out != null) {
								onClose();
							}
						}
					}); 
					
					if (first) {
						oout.close();
						onOpen();
					} else {
						Integer lastEventId = Integer.valueOf(request.queryString().get("lastEventId")[0]);
						List<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
						for (Map<String, Object> message : cache) {
							if (lastEventId < (Integer) message.get("id")) {
								temp.add(message);
							}
						}
						cache.clear();

						if (!temp.isEmpty()) {
							Collections.sort(temp, new Comparator<Map<String, Object>>() {
								@Override
								public int compare(Map<String, Object> o1, Map<String, Object> o2) {
									return (Integer) o1.get("id") > (Integer) o2.get("id") ? 1 : -1;
								}
							});
							logger.debug("With the last event id {}, flushing cached messages {}", lastEventId, temp);
							write(Json.stringify(Json.toJson(temp)));
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
		protected void write(String it) {
			if (out != null) {
				if (param("transport").equals("longpolljsonp")) {
					out.write(param("callback"));
					out.write("(");
					try {
						out.write(mapper.writeValueAsString(it));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					out.write(");");
				} else {
					out.write(it);
				}
				out.close();
				out = null;
			}
		}

	}

}
