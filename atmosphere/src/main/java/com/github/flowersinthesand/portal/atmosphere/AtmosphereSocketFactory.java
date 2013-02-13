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
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Prepare;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketFactory")
public class AtmosphereSocketFactory extends AbstractSocketFactory implements AtmosphereHandler {

	private final Logger logger = LoggerFactory.getLogger(AtmosphereSocketFactory.class);
	@Wire
	private String url;
	@Wire
	private AtmosphereFramework framework;

	@Prepare
	public void prepare() {
		framework.addAtmosphereHandler(url, this);
	}

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		AtmosphereResponse response = resource.getResponse();

		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");

		response.setHeader("Expires", "-1");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");

		response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") == null ? "*" : request.getHeader("Origin"));
		response.setHeader("Access-Control-Allow-Credentials", "true");

		if (request.getMethod().equalsIgnoreCase("GET")) {
			if (request.getParameter("abortion") != null) {
				abort(request.getParameter("id"));
			} else {
				open(resource);
			}
		} else if (request.getMethod().equalsIgnoreCase("POST")) {
			String raw = read(request.getReader());
			logger.debug("POST message body {}", raw);
			fire(raw.startsWith("data=") ? raw.substring("data=".length()) : raw);
		}
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		
		try {
			char[] buffer = new char[4096];
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
		
		return out.toString();
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
		
		writer.print((String) event.getMessage());
		writer.flush();
		
		if (request.getParameter("transport").startsWith("longpoll")) {
			request.setAttribute("used", true);
			resource.resume();
		}
	}

	@Override
	public void destroy() {}
	
	private void open(final AtmosphereResource resource) {
		final AtmosphereRequest request = resource.getRequest();
		final String id = request.getParameter("id");
		final String transport = request.getParameter("transport");

		if (transport.equals("ws")) {
			sockets.put(id, new WsSocket(resource));
		} else if (transport.equals("sse") || transport.startsWith("stream")) {
			sockets.put(id, new StreamSocket(resource));
		} else if (transport.startsWith("longpoll") && "1".equals(request.getParameter("count"))) {
			sockets.put(id, new LongPollSocket(resource));
		}

		resource.addEventListener(new WebSocketEventListenerAdapter() {
			@Override
			public void onSuspend(AtmosphereResourceEvent event) {
				((AtmosphereSocket) sockets.get(id)).onSuspend(resource);
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
							|| (transport.startsWith("longpoll") && !"1".equals(request.getParameter("count")) && request.getAttribute("used") == null)) {
						((AtmosphereSocket) sockets.get(id)).onClose();
					}
				}
			}
		})
		.suspend();
	}

	private static class BroadcasterFactoryHolder {
		static BroadcasterFactory defaults = BroadcasterFactory.getDefault();
	}

	private static BroadcasterFactory broadcasterFactory() {
		return BroadcasterFactoryHolder.defaults;
	}
	
	class AtmosphereSocket extends AbstractSocket {

		protected Broadcaster broadcaster;

		public AtmosphereSocket(AtmosphereResource resource) {
			this.params = params(resource.getRequest().getParameterMap());
			this.broadcaster = broadcasterFactory().get(id()).addAtmosphereResource(resource);
		}

		@Override
		protected void transmit(String it) {
			broadcaster.broadcast(it);
		}

		@Override
		protected void disconnect() {
			if (broadcaster.getAtmosphereResources().size() == 0) {
				onClose();
			} else {
				for (AtmosphereResource r : broadcaster.getAtmosphereResources()) {
					r.resume();
					try {
						r.close();
					} catch (IOException e) {
						logger.warn("", e);
					}
				}
			}
		}

		@Override
		protected void onClose() {
			super.onClose();
			broadcaster.destroy();
		}
		
		public void onSuspend(AtmosphereResource resource) {}
		
	}

	class WsSocket extends AtmosphereSocket {

		public WsSocket(AtmosphereResource resource) {
			super(resource);
		}
		
		@Override
		public void onSuspend(AtmosphereResource resource) {
			onOpen();
		}

	}
	
	class StreamSocket extends AtmosphereSocket {

		public StreamSocket(AtmosphereResource resource) {
			super(resource);
			this.isAndroid = isAndroid(resource.getRequest().getHeader("user-agent"));
			resource.getResponse().setContentType("text/" + ("sse".equals(param("transport")) ? "event-stream" : "plain"));
		}

		@Override
		public void onSuspend(AtmosphereResource resource) {
			PrintWriter writer;
			try {
				writer = resource.getResponse().getWriter();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			writer.print(padding2K);
			if (isAndroid) {
				writer.print(padding2K);
			}
			writer.print('\n');
			writer.flush();
			
			onOpen();
		}

	}
	
	class LongPollSocket extends AtmosphereSocket {
		
		private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();
		
		public LongPollSocket(AtmosphereResource resource) {
			super(resource);
			resource.getResponse().setContentType("text/" + ("longpolljsonp".equals(param("transport")) ? "javascript" : "plain"));
		}

		@Override
		public void onSuspend(AtmosphereResource resource) {
			AtmosphereRequest request = resource.getRequest();
			
			if ("1".equals(request.getParameter("count"))) {
				resource.resume();
				onOpen();
			} else {
				this.broadcaster.addAtmosphereResource(resource);

				// TODO enhance
				Integer lastEventId = Integer.valueOf(request.getParameter("lastEventId"));
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

		@Override
		protected void cache(Map<String, Object> message) {
			cache.add(message);
		}

	}

}
