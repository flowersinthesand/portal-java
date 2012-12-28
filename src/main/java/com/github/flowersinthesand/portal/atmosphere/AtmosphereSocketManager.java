/*
 * Copyright 2012 Donghwan Kim
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.SocketManager;

public class AtmosphereSocketManager implements SocketManager, AtmosphereHandler {

	private final Logger logger = LoggerFactory.getLogger(AtmosphereSocketManager.class);
	private Map<String, AtmosphereSocket> sockets = new ConcurrentHashMap<String, AtmosphereSocket>();
	private BroadcasterFactory broadcasterFactory = BroadcasterFactory.getDefault();
	private ObjectMapper mapper = new ObjectMapper();
	private App app;

	@Override
	public void onRequest(final AtmosphereResource resource) throws IOException {
		final AtmosphereRequest request = resource.getRequest();
		final AtmosphereResponse response = resource.getResponse();
		
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		
		if (request.getMethod().equalsIgnoreCase("GET")) {
			final String id = request.getParameter("id");
			final String transport = request.getParameter("transport");
			final boolean firstLongPoll = transport.startsWith("longpoll") && "1".equals(request.getParameter("count"));
			final PrintWriter writer = response.getWriter();

			resource.addEventListener(new AtmosphereResourceEventListener() {
				@Override
				public void onPreSuspend(AtmosphereResourceEvent event) {
					if (transport.equals("sse") || transport.startsWith("stream")) {
						response.setContentType("text/" + ("sse".equals(transport) ? "event-stream" : "plain"));
						for (int i = 0; i < 2000; i++) {
							writer.print(' ');
						}
						writer.print("\n");
						writer.flush();
					} else if (transport.startsWith("longpoll")) {
						response.setContentType("text/" + ("longpolljsonp".equals(transport) ? "javascript" : "plain"));
					}
				}

				@Override
				public void onSuspend(AtmosphereResourceEvent event) {
					if (!transport.startsWith("longpoll")) {
						start(id, resource);
					} else {
						if (firstLongPoll) {
							start(id, resource);
							resource.resume();
						} else {
							Broadcaster broadcaster = broadcasterFactory.lookup(id); 
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
				public void onBroadcast(AtmosphereResourceEvent event) {}

				@Override
				public void onThrowable(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				@Override
				public void onResume(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				@Override
				public void onDisconnect(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				private void cleanup(AtmosphereResourceEvent event) {
					if (sockets.containsKey(id) && (!transport.startsWith("longpoll") || (!firstLongPoll && request.getAttribute("used") == null))) {
						end(id);
					}
				}
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
		if (event.getMessage() instanceof List) {
			for (Object message : (List<?>) event.getMessage()) {
				format(writer, transport, message, jsonp);
			}
		} else {
			format(writer, transport, event.getMessage(), jsonp);
		}

		writer.flush();
		if (transport.startsWith("longpoll")) {
			request.setAttribute("used", true);
			resource.resume();
		}
	}

	@Override
	public void destroy() {}

	private void start(String id, AtmosphereResource resource) {
		String query = resource.getRequest().getQueryString();
		logger.info("Socket#{} has been opened, query: {}", id, query);
		
		AtmosphereSocket socket = new AtmosphereSocket(id, app, query);

		broadcasterFactory.get(id).addAtmosphereResource(resource);
		sockets.put(id, socket);
		socket.setHeartbeatTimer();

		app.eventDispatcher().fire("open", socket);
	}

	private void end(String id) {
		logger.info("Socket#{} has been closed", id);

		AtmosphereSocket socket = sockets.get(id);

		broadcasterFactory.lookup(socket.id()).destroy();
		sockets.remove(socket.id());
		Timer heartbeatTimer = socket.heartbeatTimer();
		if (heartbeatTimer != null) {
			heartbeatTimer.cancel();
		}
		for (Room room : app.findAllRoom().values()) {
			room.remove(socket);
		}

		app.eventDispatcher().fire("close", socket);
	}

	private void fire(String raw) throws IOException {
		final Map<String, Object> message = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		final AtmosphereSocket socket = sockets.get(message.get("socket"));
		String type = (String) message.get("type");
		Object data = message.get("data");
		boolean reply = message.containsKey("reply") && (Boolean) message.get("reply");
		logger.info("Receiving an event {}", message);

		if (type.equals("heartbeat")) {
			logger.debug("Handling heartbeat");
			if (socket.heartbeatTimer() != null) {
				socket.setHeartbeatTimer();
				socket.send("heartbeat");
			}
		} else if (type.equals("reply")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> replyMessage = (Map<String, Object>) data;
			Integer replyEventId = (Integer) replyMessage.get("id");
			Object replyData = replyMessage.get("data");
			if (socket.callbacks().containsKey(replyEventId)) {
				logger.debug("Executing the reply function corresponding to the event#{} with the data {}", replyEventId, replyData);
				socket.callbacks().get(replyEventId).call(replyData);
				socket.callbacks().remove(replyEventId);
			}
		}

		if (!reply) {
			app.eventDispatcher().fire(type, socket, data);
		} else {
			app.eventDispatcher().fire(type, socket, data, new Fn.Callback1<Object>() {
				@Override
				public void call(Object arg1) {
					Map<String, Object> replyData = new LinkedHashMap<String, Object>();
					replyData.put("id", message.get("id"));
					replyData.put("data", arg1);

					logger.debug("Sending the reply event with the data {}", replyData);
					socket.send("reply", replyData);
				}
			});
		}
	}

	private void format(PrintWriter writer, String transport, Object message, String jsonp) throws IOException {
		String data = mapper.writeValueAsString(message);
		logger.debug("Formatting data {} for {} transport", data, transport);

		if (transport.equals("ws")) {
			writer.print(data);
		} else if (transport.equals("sse") || transport.startsWith("stream")) {
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

	@Override
	public boolean opened(Socket socket) {
		return sockets.containsValue(socket);
	}

	@Override
	public void send(Socket s, String event, Object data) {
		AtmosphereSocket socket = (AtmosphereSocket) s;

		socket.eventId().incrementAndGet();

		Map<String, Object> message = rawMessage(socket.eventId().get(), event, data, false);
		logger.info("Sending an event {}", message);
		broadcasterFactory.lookup(socket.id()).broadcast(socket.cache(message));
	}

	@Override
	public void send(Socket s, String event, Object data, final Fn.Callback callback) {
		AtmosphereSocket socket = (AtmosphereSocket) s;

		socket.eventId().incrementAndGet();
		socket.callbacks().put(socket.eventId().get(), new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				callback.call();
			}
		});

		Map<String, Object> message = rawMessage(socket.eventId().get(), event, data, true);
		logger.info("Sending an event {}", message);
		broadcasterFactory.lookup(socket.id()).broadcast(socket.cache(message));
	}

	@Override
	public <A> void send(Socket s, String event, Object data, final Fn.Callback1<A> callback) {
		AtmosphereSocket socket = (AtmosphereSocket) s;

		socket.eventId().incrementAndGet();
		socket.callbacks().put(socket.eventId().get(), new Fn.Callback1<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public void call(Object arg1) {
				((Fn.Callback1<Object>) callback).call(arg1);
			}
		});

		Map<String, Object> message = rawMessage(socket.eventId().get(), event, data, true);
		logger.info("Sending an event {}", message);
		broadcasterFactory.lookup(socket.id()).broadcast(socket.cache(message));
	}

	private Map<String, Object> rawMessage(int id, String type, Object data, boolean reply) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();
		message.put("id", id);
		message.put("type", type);
		message.put("data", data);
		message.put("reply", reply);

		return message;
	}

	@Override
	public void close(Socket socket) {
		logger.info("Closing socket#{}", socket.id());
		for (AtmosphereResource r : broadcasterFactory.lookup(socket.id()).getAtmosphereResources()) {
			r.resume();
			try {
				// TODO to disconnect a websocket connection
				((AtmosphereResourceImpl) r).cancel();
			} catch (IOException e) {
				logger.warn("", e);
			}
		}
		sockets.remove(socket.id());
	}

	public void setApp(App app) {
		this.app = app;
	}

}
