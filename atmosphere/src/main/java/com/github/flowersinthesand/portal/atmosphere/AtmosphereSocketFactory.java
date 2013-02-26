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

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketFactory")
public class AtmosphereSocketFactory extends AbstractSocketFactory {

	void open(final AtmosphereResource resource) {
		final AtmosphereRequest req = resource.getRequest();
		final String when = req.getParameter("when");
		final String id = req.getParameter("id");
		final String transport = req.getParameter("transport");

		if (transport.equals("ws")) {
			sockets.put(id, new WsSocket(resource));
		} else if (transport.equals("sse") || transport.startsWith("stream")) {
			sockets.put(id, new StreamSocket(resource));
		} else if (transport.startsWith("longpoll") && when.equals("open")) {
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
							|| (transport.startsWith("longpoll") && when.equals("poll") && req.getAttribute("used") == null)) {
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
						throw new RuntimeException(e);
					}
				}
			}
		}

		@Override
		public void onClose() {
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
			resource.getResponse().setContentType(streamContentType());
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
		
		public LongPollSocket(AtmosphereResource resource) {
			super(resource);
			resource.getResponse().setContentType(longpollContentType());
		}

		@Override
		public void onSuspend(AtmosphereResource resource) {
			AtmosphereRequest req = resource.getRequest();
			String when = req.getParameter("when"); 
			
			if (when.equals("open")) {
				resource.resume();
				onOpen();
			} else if (when.equals("poll")) {
				this.broadcaster.addAtmosphereResource(resource);
				retrieveCache(req.getParameter("lastEventIds"));
			}
		}

	}

}
