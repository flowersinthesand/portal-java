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
package org.flowersinthesand.portal.atmosphere;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.BroadcasterFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.flowersinthesand.portal.App;
import org.flowersinthesand.portal.Events;
import org.flowersinthesand.portal.Fn.Callback;
import org.flowersinthesand.portal.Fn.Callback1;
import org.flowersinthesand.portal.Room;
import org.flowersinthesand.portal.Socket;
import org.flowersinthesand.portal.Sockets;

public class AtmosphereSockets implements Sockets, AtmosphereHandler {
	
	private Map<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	private ObjectMapper mapper = new ObjectMapper();
	private App app;
	
	public AtmosphereSockets(App app) {
		this.app = app;
	}

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		final AtmosphereRequest request = resource.getRequest();
		final AtmosphereResponse response = resource.getResponse();

		if (request.getMethod().equalsIgnoreCase("GET")) {
			final String id = request.getParameter("id");
			final String transport = request.getParameter("transport");
			final boolean first = "1".equals(request.getParameter("count"));
			final PrintWriter writer = response.getWriter();
			
			resource.addEventListener(new AtmosphereResourceEventListener() {
				@Override
				public void onPreSuspend(AtmosphereResourceEvent event) {
					response.setCharacterEncoding("utf-8");
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
					if (!transport.startsWith("longpoll") || first) {
						onOpen(id, event.getResource().getRequest().getParameterMap());
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
					if (!transport.startsWith("longpoll") || (!first && !event.getResource().getResponse().isCommitted())) {
						onClose(sockets.get(id));
					}
				}
			})
			.suspend();
		} else if (request.getMethod().equalsIgnoreCase("POST")) {
			String data = request.getReader().readLine();
			if (data != null) {
				data = data.startsWith("data=") ? data.substring("data=".length()) : data;
				Map<String, Object> message = mapper.readValue(data, new TypeReference<Map<String, Object>>() {});
				app.events().fire((String) message.get("type"), sockets.get(message.get("socket")), message.get("data"));
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent event) throws IOException {
		AtmosphereResource resource = event.getResource();
		AtmosphereRequest request = resource.getRequest();
		AtmosphereResponse response = resource.getResponse();
		if (event.getMessage() == null || event.isCancelled() || event.isResuming() || event.isResumedOnTimeout() || request.destroyed()) {
			return;
		}

		PrintWriter writer = response.getWriter();
		String transport = request.getParameter("transport");

		format(writer, transport, event.getMessage(), request.getParameter("callback"));
		writer.flush();
		if (transport.startsWith("longpoll")) {
			resource.resume();
		}
	}

	@Override
	public void destroy() {}

	private void onOpen(String id, Map<String, String[]> params) {
		Socket socket = new Socket(id, app, params);
		
		BroadcasterFactory.getDefault().get(id);
		sockets.put(id, socket);
		app.events().fire("open", socket);
	}
	
	private void onClose(Socket socket) {
		BroadcasterFactory.getDefault().remove(socket.id());
		sockets.remove(socket.id());
		for (Room room : app.rooms().values()) {
			room.remove(socket);
		}
		app.events().fire("close", socket);
	}

	private void format(PrintWriter writer, String transport, Object message, String jsonp)
			throws JsonGenerationException, JsonMappingException, IOException {
		String data = mapper.writeValueAsString(message);
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
				writer.print(")");
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
	public void send(Socket socket, String event, Object data) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();
		message.put("type", event);
		message.put("data", data);
		message.put("reply", false);
		BroadcasterFactory.getDefault().lookup(socket.id()).broadcast(message);
	}

	@Override
	public void send(Socket socket, String event, Object data, Callback callback) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();
		message.put("type", event);
		message.put("data", data);
		message.put("reply", true);
		// TODO deal with callback
		BroadcasterFactory.getDefault().lookup(socket.id()).broadcast(message);
	}

	@Override
	public <A> void send(Socket socket, String event, Object data, Callback1<A> callback) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();
		message.put("type", event);
		message.put("data", data);
		message.put("reply", true);
		// TODO deal with callback1
		BroadcasterFactory.getDefault().lookup(socket.id()).broadcast(message);
	}

	@Override
	public void close(Socket socket) {
		BroadcasterFactory.getDefault().lookup(socket.id()).resumeAll();
		sockets.remove(socket.id());
	}

}
