/*
 * Copyright 2012-2014 Donghwan Kim
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
package io.github.flowersinthesand.portal;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.ConcurrentActions;
import io.github.flowersinthesand.wes.Data;
import io.github.flowersinthesand.wes.HttpStatus;
import io.github.flowersinthesand.wes.ServerHttpExchange;
import io.github.flowersinthesand.wes.ServerWebSocket;
import io.github.flowersinthesand.wes.VoidAction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of {@link Server}.
 * <p>
 * This implementation provides and manages {@link Socket} processing HTTP
 * request and WebSocket following the portal protocol.
 * <p>
 * As options, the following methods can be overridden.
 * <ul>
 * <li>{@link DefaultServer#parseURI(String)}
 * <li>{@link DefaultServer#parseEvent(String)}
 * <li>{@link DefaultServer#stringifyEvent(Map)}
 * </ul>
 * 
 * @author Donghwan Kim
 */
public class DefaultServer implements Server {

	private final Logger log = LoggerFactory.getLogger(DefaultServer.class);
	private ConcurrentMap<String, DefaultSocket> sockets = new ConcurrentHashMap<>();
	private Actions<Socket> socketActions = new ConcurrentActions<>();

	private Action<ServerHttpExchange> httpAction = new Action<ServerHttpExchange>() {
		@Override
		public void on(final ServerHttpExchange http) {
			switch (http.method()) {
			case "GET":
				Map<String, String> params = parseURI(http.uri());
				setNocache(http);
				setCors(http);
				switch (params.get("when")) {
				case "open":
					switch (params.get("transport")) {
					case "sse":
					case "streamxhr":
					case "streamxdr":
					case "streamiframe":
						socketActions.fire(new DefaultSocket(new StreamTransport(params, http)));
						break;
					case "longpollajax":
					case "longpollxdr":
					case "longpolljsonp":
						socketActions.fire(new DefaultSocket(new LongpollTransport(params, http)));
						break;
					default:
						log.error("Transport, {}, is not supported", params.get("transport"));
						http.setStatus(HttpStatus.NOT_IMPLEMENTED).close();
					}
					break;
				case "poll": {
					String id = params.get("id");
					DefaultSocket socket = sockets.get(id);
					if (socket != null) {
						Transport transport = socket.transport;
						if (transport instanceof LongpollTransport) {
							((LongpollTransport) transport).refresh(http);
						} else {
							log.error("Non-long polling transport#{} sent poll request", id);
							http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).close();
						}
					} else {
						log.error("Long polling transport#{} is not found in poll request", id);
						http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).close();
					}
					break; }
				case "abort": {
					String id = params.get("id");
					Socket socket = sockets.get(id);
					if (socket != null) {
						socket.close();
					}
					http.setResponseHeader("content-type", "text/javascript; charset=utf-8").close();
					break; }
				default:
					log.error("when, {}, is not supported", params.get("when"));
					http.setStatus(HttpStatus.NOT_IMPLEMENTED).close();
					break;
				}
				break;
			case "POST":
				setNocache(http);
				setCors(http);
				http.bodyAction(new Action<Data>() {
					@Override
					public void on(Data body) {
						String data = body.as(String.class).substring("data=".length());
						String id = findSocketId(data);
						
						DefaultSocket socket = sockets.get(id);
						if (socket != null) {
							Transport transport = socket.transport;
							if (transport instanceof HttpTransport) {
								transport.messageActions.fire(data);
							} else {
								log.error("Non-HTTP socket#{} receives a POST message", id);
								http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
							}
						} else {
							log.error("A POST message arrived but no socket#{} is found", id);
							http.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
						}
						http.close();
					};
					
					private String findSocketId(String text) {
						Matcher matcher = Pattern.compile("\"socket\":\"([^\"]+)\"").matcher(text);
						matcher.find();
						return matcher.group(1);
					}
				});
				break;
			default:
				log.error("HTTP method, {}, is not supported", http.method());
				http.setStatus(HttpStatus.METHOD_NOT_ALLOWED).close();
				break;
			}
		}
		
		private void setNocache(ServerHttpExchange http) {
			http
			.setResponseHeader("cache-control", "no-cache, no-store, must-revalidate")
			.setResponseHeader("pragma", "no-cache")
			.setResponseHeader("expires", "0");
		}
		
		private void setCors(ServerHttpExchange http) {
			String origin = http.requestHeader("origin");
			String acrh = http.requestHeader("access-control-request-headers");
			http
			.setResponseHeader("access-control-allow-origin", origin != null ? origin : "*")
			.setResponseHeader("access-control-allow-credentials", "true");
			if (acrh != null) {
				http.setResponseHeader("access-control-allow-headers", acrh);
			}
		}
	};

	private Action<ServerWebSocket> websocketAction = new Action<ServerWebSocket>() {
		@Override
		public void on(ServerWebSocket ws) {
			Map<String, String> params = parseURI(ws.uri());
			socketActions.fire(new DefaultSocket(new WebSocketTransport(params, ws)));
		}
	};

	/**
	 * Takes a portal URI and returns a map of parameters.
	 * <p>
	 * This is a counterpart of {@code urlBuilder} of client option.
	 */
	protected Map<String, String> parseURI(String uri) {
		Map<String, String> map = new LinkedHashMap<>();
		String query = URI.create(uri).getQuery();
		if ((query == null) || (query.equals(""))) {
			return map;
		}

		String[] params = query.split("&");
		for (String param : params) {
			try {
				String[] pair = param.split("=", 2);
				String name = URLDecoder.decode(pair[0], "UTF-8");
				if (name == "") {
					continue;
				}

				map.put(name, pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "");
			} catch (UnsupportedEncodingException e) {}
		}

		return Collections.unmodifiableMap(map);
	}

	/**
	 * Takes a stringified event and returns an event object.
	 * <p>
	 * A text in argument is generated by {@code outbound} of client option and
	 * this is akin to {@code inbound} of client option.
	 */
	protected Map<String, Object> parseEvent(String text) {
		try {
			return new ObjectMapper().readValue(text, new TypeReference<Map<String, Object>>() {});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Takes an event object and returns a stringified event.
	 * <p>
	 * This is akin to {@code outbound} of client option and a returned value will
	 * be handled by {@code inbound} of client option.
	 */
	protected String stringifyEvent(Map<String, Object> event) {
		try {
			return new ObjectMapper().writeValueAsString(event);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Sentence all() {
		return new Sentence(new Action<Action<Socket>>() {
			@Override
			public void on(Action<Socket> action) {
				all(action);
			}
		});
	}
	
	@Override
	public Server all(Action<Socket> action) {
		for (Socket socket : sockets.values()) {
			action.on(socket);
		}
		return this;
	}
	
	@Override
	public Sentence byId(final String id) {
		return new Sentence(new Action<Action<Socket>>() {
			@Override
			public void on(Action<Socket> action) {
				byId(id, action);
			}
		});
	}

	@Override
	public Server byId(String id, Action<Socket> action) {
		Socket socket = sockets.get(id);
		if (socket != null) {
			action.on(socket);
		}
		return this;
	}

	@Override
	public Sentence byTag(final String... names) {
		return new Sentence(new Action<Action<Socket>>() {
			@Override
			public void on(Action<Socket> action) {
				byTag(names, action);
			}
		});
	}

	@Override
	public Server byTag(String name, Action<Socket> action) {
		return byTag(new String[] { name }, action);
	}

	@Override
	public Server byTag(String[] names, Action<Socket> action) {
		List<String> nameList = Arrays.asList(names);
		for (Socket socket : sockets.values()) {
			if (socket.tags().containsAll(nameList)) {
				action.on(socket);
			}
		}
		return this;
	}

	@Override
	public Server socketAction(Action<Socket> action) {
		socketActions.add(action);
		return this;
	}

	@Override
	public Action<ServerHttpExchange> httpAction() {
		return httpAction;
	}

	@Override
	public Action<ServerWebSocket> websocketAction() {
		return websocketAction;
	}

	private abstract class Transport {
		final Map<String, String> params;
		Actions<String> messageActions = new ConcurrentActions<>();
		Actions<Void> closeActions = new ConcurrentActions<>();

		Transport(Map<String, String> params) {
			this.params = params;
		}
		
		abstract String uri();
		abstract void send(String data);
		abstract void close();
	}

	private class WebSocketTransport extends Transport {
		final ServerWebSocket ws;
		
		WebSocketTransport(Map<String, String> params, ServerWebSocket ws) {
			super(params);
			this.ws = ws;
			ws.closeAction(new VoidAction() {
				@Override
				public void on() {
					closeActions.fire();
				}
			})
			.messageAction(new Action<Data>() {
				@Override
				public void on(Data data) {
					messageActions.fire(data.as(String.class));
				}
			});
		}
		
		@Override
		String uri() {
			return ws.uri();
		}

		@Override
		synchronized void send(String data) {
			ws.send(data);
		}

		@Override
		synchronized void close() {
			ws.close();
		}
	}
	
	private abstract class HttpTransport extends Transport {
		final ServerHttpExchange http;
		
		HttpTransport(Map<String, String> params, ServerHttpExchange http) {
			super(params);
			this.http = http;
		}
		
		@Override
		String uri() {
			return http.uri();
		}
	}

	final static String text2KB = CharBuffer.allocate(2048).toString().replace('\0', ' ');
	
	private class StreamTransport extends HttpTransport {
		final boolean isAndroidLowerThan3;
		
		StreamTransport(Map<String, String> params, ServerHttpExchange http) {
			super(params, http);
			String ua = http.requestHeader("user-agent");
			this.isAndroidLowerThan3 = ua == null ? false : ua.matches(".*Android\\s[23]\\..*");
			http.closeAction(new VoidAction() {
				@Override
				public void on() {
					closeActions.fire();
				}
			})
			.setResponseHeader("content-type",
				"text/" + (params.get("transport").equals("sse") ? "event-stream" : "plain") + "; charset=utf-8")
			.write((isAndroidLowerThan3 ? text2KB : "") + text2KB + "\n");
		}
		
		@Override
		synchronized void send(String data) {
			String payload = (isAndroidLowerThan3 ? text2KB + text2KB : "") + "";
			for (String datum : data.split("\r\n|\r|\n")) {
				payload += "data: " + datum + "\n";
			}
			payload += "\n";
			http.write(payload);
		}
		
		@Override
		synchronized void close() {
			http.close();
		}
	}
	
	private class LongpollTransport extends HttpTransport {
		AtomicReference<ServerHttpExchange> httpRef = new AtomicReference<>();
		AtomicBoolean closed = new AtomicBoolean();
		AtomicBoolean written = new AtomicBoolean();
		Set<String> buffer = new CopyOnWriteArraySet<>(); 
		AtomicReference<Timer> closeTimer = new AtomicReference<>();
		
		LongpollTransport(Map<String, String> params, ServerHttpExchange http) {
			super(params, http);
			refresh(http);
		}

		void refresh(ServerHttpExchange http) {
			final Map<String, String> parameters = parseURI(http.uri());
			http.closeAction(new VoidAction() {
				@Override
				public void on() {
					closed.set(true);
					if (parameters.get("when").equals("poll") && !written.get()) {
						closeActions.fire();
					}
					Timer timer = new Timer(true);
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							closeActions.fire();
						}
					}, 500);
					closeTimer.set(timer);
				}
			})
			.setResponseHeader("content-type", 
				"text/" + (params.get("transport").equals("longpolljsonp") ? "javascript" : "plain") + "; charset=utf-8");
			
			if (parameters.get("when").equals("open")) {
				http.close();
			} else {
				httpRef.set(http);
				closed.set(false);
				written.set(false);
				Timer timer = closeTimer.getAndSet(null);
				if (timer != null) {
					timer.cancel();
				}
				if (parameters.containsKey("lastEventIds")) {
					String[] lastEventIds = parameters.get("lastEventIds").split(",");
					for (String eventId : lastEventIds) {
						for (String message : buffer) {
							if (eventId.equals(findEventId(message))) {
								buffer.remove(message);
							}
						}
					}
					if (!buffer.isEmpty()) {
						Iterator<String> iterator = buffer.iterator();
						String string = iterator.next();
						while (iterator.hasNext()) {
							string += "," + iterator.next();
						}
						send("[" + string + "]");
					}
				}
			}
		}

		private String findEventId(String text) {
			Matcher matcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(text);
			matcher.find();
			return matcher.group(1);
		}

		@Override
		synchronized void send(String data) {
			if (!data.startsWith("[")) {
				buffer.add(data);
			}
			ServerHttpExchange http = httpRef.getAndSet(null);
			if (http != null && !closed.get()) {
				written.set(true);
				String payload;
				try {
					payload = params.get("transport").equals("longpolljsonp") ? 
						params.get("callback") + "(" + new ObjectMapper().writeValueAsString(data) + ");" : 
						data; 
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
				http.close(payload);
			}
		}
		
		@Override
		synchronized void close() {
			ServerHttpExchange http = httpRef.getAndSet(null);
			if (http != null && !closed.get()) {
				http.close();
			}
		}
	}
	
	private class DefaultSocket implements Socket {
		final Transport transport;
		Set<String> tags = new CopyOnWriteArraySet<>();
		ConcurrentMap<String, Actions<Object>> actionsMap = new ConcurrentHashMap<>();
		ConcurrentMap<String, Action<Object>> replyMap = new ConcurrentHashMap<>();

		DefaultSocket(final Transport transport) {
			this.transport = transport;
			transport.closeActions.add(new VoidAction() {
				@Override
				public void on() {
					sockets.remove(transport.params.get("id"));
					Actions<Object> closeActions = actionsMap.get("close");
					if (closeActions != null) {
						closeActions.fire();
					}
				}
			});
			transport.messageActions.add(new Action<String>() {
				@Override
				public void on(String text) {
					final Map<String, Object> event = parseEvent(text);
					Actions<Object> actions = actionsMap.get(event.get("type"));
					if (actions != null) {
						if ((Boolean) event.get("reply")) {
							actions.fire(new Reply<Object>() {
								@Override
								public Object data() {
									return event.get("data");
								}

								@Override
								public void done() {
									done(null);
								}

								@Override
								public void done(Object value) {
									sendReply(value, false);
								}

								@Override
								public void fail() {
									fail(null);
								}

								@Override
								public void fail(Object value) {
									sendReply(value, true);
								}

								AtomicBoolean sent = new AtomicBoolean();
								private void sendReply(Object value, boolean exception) {
									if (sent.compareAndSet(false, true)) {
										Map<String, Object> result = new LinkedHashMap<String, Object>();
										result.put("id", event.get("id"));
										result.put("data", value);
										result.put("exception", exception);
										send("reply", result);
									}
								}
							});
						} else {
							actions.fire(event.get("data"));
						}
					}
				}
			});
			on("reply", new Action<Map<String, Object>>() {
				@Override
				public void on(Map<String, Object> info) {
					Action<Object> reply = replyMap.remove(info.get("id"));
					if (reply != null) {
						reply.on(info.get("data"));
					} else {
						log.error("Reply callback not found in socket#{} with info, {}", id(), info);
					}
				}
			});
			try {
				new HeartbeatHelper(Long.valueOf(transport.params.get("heartbeat")));
			} catch (NumberFormatException e) {}
			
			sockets.put(id(), this);
		}
		
		class HeartbeatHelper {
			final long delay;
			AtomicReference<Timer> timer = new AtomicReference<>();
			
			HeartbeatHelper(long delay) {
				this.delay = delay;
				timer.set(createTimer());
				on("heartbeat", new VoidAction() {
					@Override
					public void on() {
						timer.getAndSet(createTimer()).cancel();
						send("heartbeat");
					}
				});
			}
			
			Timer createTimer() {
				Timer timer = new Timer(true);
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						close();
					}
				}, delay);
				return timer;
			}
		}

		@Override
		public String id() {
			return transport.params.get("id");
		}

		@Override
		public String uri() {
			return transport.uri();
		}

		@Override
		public Set<String> tags() {
			return tags;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Socket on(String event, Action<T> action) {
			Actions<Object> actions = actionsMap.get(event);
			if (actions == null) {
				Actions<Object> value = new ConcurrentActions<>();
				actions = actionsMap.putIfAbsent(event, value);
				if (actions == null) {
					actions = value;
				}
			}
			actions.add((Action<Object>) action);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Socket off(String event, Action<T> action) {
			Actions<Object> actions = actionsMap.get(event);
			if (actions != null) {
				actions.remove((Action<Object>) action);
			}
			return this;
		}

		@Override
		public Socket send(String event) {
			return send(event, null);
		}

		@Override
		public Socket send(String event, Object data) {
			return send(event, data, null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Socket send(String type, Object data, Action<T> reply) {
			String eventId = UUID.randomUUID().toString();
			Map<String, Object> event = new LinkedHashMap<String, Object>();

			event.put("id", eventId);
			event.put("type", type);
			event.put("data", data);
			event.put("reply", reply != null);
			
			String text = stringifyEvent(event);
			transport.send(text);
			if (reply != null) {
				replyMap.put(eventId, (Action<Object>) reply);
			}
			return this;
		}

		@Override
		public Socket close() {
			transport.close();
			return this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id() == null) ? 0 : id().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultSocket other = (DefaultSocket) obj;
			if (id() == null) {
				if (other.id() != null)
					return false;
			} else if (!id().equals(other.id()))
				return false;
			return true;
		}
	}

}
