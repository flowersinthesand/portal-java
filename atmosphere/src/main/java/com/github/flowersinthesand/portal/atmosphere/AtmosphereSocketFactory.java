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
package com.github.flowersinthesand.portal.atmosphere;

import java.io.IOException;
import java.io.PrintWriter;
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

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListener;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Prepare;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.SocketFactory;
import com.github.flowersinthesand.portal.support.ReplyHandler;

@Bean("socketFactory")
public class AtmosphereSocketFactory implements AtmosphereHandler, SocketFactory {

	private static final String padding2K = CharBuffer.allocate(2048).toString().replace('\0', ' ');

	private static class BroadcasterFactoryHolder {
		static BroadcasterFactory defaults = BroadcasterFactory.getDefault();
	}

	private static BroadcasterFactory broadcasterFactory() {
		return BroadcasterFactoryHolder.defaults;
	}

	private final Logger logger = LoggerFactory.getLogger(AtmosphereSocketFactory.class);
	private ObjectMapper mapper = new ObjectMapper();
	private Map<String, AtmosphereSocket> sockets = new ConcurrentHashMap<String, AtmosphereSocket>();
	@Wire
	private String url;
	@Wire
	private AtmosphereFramework framework;
	@Wire
	private Dispatcher dispatcher;
	@Wire
	private ReplyHandler replyHandler;

	@Prepare
	public void prepare() {
		framework.addAtmosphereHandler(url, this);
	}

	@Override
	public void onRequest(final AtmosphereResource resource) throws IOException {
		final AtmosphereRequest request = resource.getRequest();
		final AtmosphereResponse response = resource.getResponse();

		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");

		response.setHeader("Expires", "-1");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");

		response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") == null ? "*" : request.getHeader("Origin"));
		response.setHeader("Access-Control-Allow-Credentials", "true");

		if (request.getMethod().equalsIgnoreCase("GET")) {
			final String id = request.getParameter("id");
			final String transport = request.getParameter("transport");
			final boolean firstLongPoll = transport.startsWith("longpoll") && "1".equals(request.getParameter("count"));
			final PrintWriter writer = response.getWriter();

			resource.addEventListener(new WebSocketEventListener() {
				@Override
				public void onSuspend(AtmosphereResourceEvent event) {
					if (transport.equals("ws") || transport.equals("sse") || transport.startsWith("stream")) {
						start(id, resource);
						if (transport.equals("sse") || transport.startsWith("stream")) {
							response.setContentType("text/" + ("sse".equals(transport) ? "event-stream" : "plain"));
							writer.print(padding2K);
							// Android 2. || Android 3.
							if (request.getHeader("user-agent").matches(".*Android\\s[23]\\..*")) {
								writer.print(padding2K);
							}
							writer.print('\n');
							writer.flush();
						}
						dispatcher.fire("open", sockets.get(id));
					} else if (transport.startsWith("longpoll")) {
						response.setContentType("text/" + ("longpolljsonp".equals(transport) ? "javascript" : "plain"));
						if (firstLongPoll) {
							start(id, resource);
							resource.resume();
							dispatcher.fire("open", sockets.get(id));
						} else {
							Broadcaster broadcaster = broadcasterFactory().lookup(id);
							broadcaster.addAtmosphereResource(resource);

							Integer lastEventId = Integer.valueOf(request.getParameter("lastEventId"));
							Set<Map<String, Object>> original = sockets.get(id).cache();
							List<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
							for (Map<String, Object> message : original) {
								if (lastEventId < (Integer) message.get("id")) {
									temp.add(message);
								}
							}
							original.clear();

							if (!temp.isEmpty()) {
								Collections.sort(temp, new Comparator<Map<String, Object>>() {
									@Override
									public int compare(Map<String, Object> o1, Map<String, Object> o2) {
										return (Integer) o1.get("id") > (Integer) o2.get("id") ? 1 : -1;
									}
								});
								logger.debug("With the last event id {}, flushing cached messages {}", lastEventId, temp);
								broadcaster.broadcast(temp);
							}
						}
					}
				}

				@Override
				public void onThrowable(AtmosphereResourceEvent event) {
					cleanup();
				}

				@Override
				public void onResume(AtmosphereResourceEvent event) {
					cleanup();
				}

				@Override
				public void onDisconnect(AtmosphereResourceEvent event) {
					cleanup();
				}

				@Override
				public void onDisconnect(@SuppressWarnings("rawtypes") WebSocketEvent event) {
					cleanup();
				}

				@Override
				public void onClose(@SuppressWarnings("rawtypes") WebSocketEvent event) {
					cleanup();
				}

				private void cleanup() {
					if (sockets.containsKey(id)) {
						if ((transport.equals("ws") || transport.equals("sse") || transport.startsWith("stream"))
								|| (transport.startsWith("longpoll") && !firstLongPoll && request.getAttribute("used") == null)) {
							Socket socket = sockets.get(id);
							end(id);
							dispatcher.fire("close", socket);
						}
					}
				}

				@Override
				public void onPreSuspend(AtmosphereResourceEvent event) {}

				@Override
				public void onBroadcast(AtmosphereResourceEvent event) {}

				@Override
				public void onHandshake(@SuppressWarnings("rawtypes") WebSocketEvent event) {}

				@Override
				public void onMessage(@SuppressWarnings("rawtypes") WebSocketEvent event) {}

				@Override
				public void onControl(@SuppressWarnings("rawtypes") WebSocketEvent event) {}

				@Override
				public void onConnect(@SuppressWarnings("rawtypes") WebSocketEvent event) {}
			})
			.suspend();
		} else if (request.getMethod().equalsIgnoreCase("POST")) {
			String data = request.getReader().readLine();
			if (data != null) {
				logger.debug("POST message body {}", data);
				fire(data.startsWith("data=") ? data.substring("data=".length()) : data);
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent event) throws IOException {
		if (event.getMessage() == null || event.isCancelled() || event.isResuming() || event.isResumedOnTimeout()) {
			return;
		}

		AtmosphereResource resource = event.getResource();
		AtmosphereRequest request = resource.getRequest();
		AtmosphereResponse response = resource.getResponse();

		PrintWriter writer = response.getWriter();
		String transport = request.getParameter("transport");

		String jsonp = request.getParameter("callback");
		String userAgent = request.getHeader("user-agent");
		if (event.getMessage() instanceof List) {
			for (Object message : (List<?>) event.getMessage()) {
				format(writer, transport, message, jsonp, userAgent);
			}
		} else {
			format(writer, transport, event.getMessage(), jsonp, userAgent);
		}

		writer.flush();
		if (transport.startsWith("longpoll")) {
			request.setAttribute("used", true);
			resource.resume();
		}
	}

	@Override
	public void destroy() {}
	
	@Override
	public Socket find(String id) {
		return sockets.get(id);
	}

	private void start(String id, AtmosphereResource resource) {
		Map<String, String> params = new LinkedHashMap<String, String>();
		for (Entry<String, String[]> entry : resource.getRequest().getParameterMap().entrySet()) {
			params.put(entry.getKey(), entry.getValue()[0]);
		}

		logger.info("Socket#{} has been opened, params: {}", id, params);
		sockets.put(id, new AtmosphereSocket(params, broadcasterFactory().get(id).addAtmosphereResource(resource)));
	}

	private void end(String id) {
		logger.info("Socket#{} has been closed", id);
		broadcasterFactory().lookup(id).destroy();
		sockets.remove(id);
	}

	private void fire(String raw) throws IOException {
		Map<String, Object> m = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		logger.info("Receiving an event {}", m);
		dispatcher.fire((String) m.get("type"), sockets.get(m.get("socket")), m.get("data"), (Boolean) m.get("reply") ? (Integer) m.get("id") : 0);
	}

	private void format(PrintWriter writer, String transport, Object message, String jsonp, String userAgent) throws IOException {
		String data = mapper.writeValueAsString(message);
		logger.debug("Formatting data {} for {} transport", data, transport);

		if (transport.equals("ws")) {
			writer.print(data);
		} else if (transport.equals("sse") || transport.startsWith("stream")) {
			// Android 2. || Android 3.
			if (userAgent.matches(".*Android\\s[23]\\..*")) {
				writer.print(padding2K);
				writer.print(padding2K);
			}
			for (String datum : data.split("\r\n|\r|\n")) {
				writer.print("data: ");
				writer.print(datum);
				writer.print("\n");
			}
			writer.print("\n");
		} else if (transport.startsWith("longpoll")) {
			if (transport.equals("longpolljsonp")) {
				writer.print(jsonp);
				writer.print("(");
				writer.print(mapper.writeValueAsString(data));
				writer.print(");");
			} else {
				writer.print(data);
			}
		}
	}

	class AtmosphereSocket implements Socket {

		private String id;
		private Map<String, String> params = new LinkedHashMap<String, String>();
		private Broadcaster broadcaster;
		private AtomicInteger eventId = new AtomicInteger();
		private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();

		public AtmosphereSocket(Map<String, String> params, Broadcaster broadcaster) {
			this.id = params.get("id");
			this.params.putAll(params);
			this.broadcaster = broadcaster;
		}

		@Override
		public String id() {
			return id;
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
			replyHandler.set(id, eventId.get(), callback);
			return this;
		}

		@Override
		public Socket send(String event, Object data, Fn.Callback1<?> callback) {
			doSend(event, data, true);
			replyHandler.set(id, eventId.get(), callback);
			return this;
		}

		private void doSend(String type, Object data, boolean reply) {
			Map<String, Object> message = new LinkedHashMap<String, Object>();

			message.put("id", eventId.incrementAndGet());
			message.put("type", type);
			message.put("data", data);
			message.put("reply", reply);

			logger.info("Socket#{} is sending an event {}", id, message);
			broadcaster.broadcast(cache(message));
		}

		@Override
		public Socket close() {
			logger.info("Closing socket#{}", id);
			for (AtmosphereResource r : broadcaster.getAtmosphereResources()) {
				r.resume();
				try {
					r.close();
				} catch (IOException e) {
					logger.warn("", e);
				}
			}
			return this;
		}

		public Set<Map<String, Object>> cache() {
			return cache;
		}

		public Map<String, Object> cache(Map<String, Object> message) {
			if (param("transport").startsWith("longpoll")) {
				cache.add(message);
			}
			return message;
		}

	}

}
