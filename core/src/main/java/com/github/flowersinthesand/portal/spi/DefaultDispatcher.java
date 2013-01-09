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
package com.github.flowersinthesand.portal.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;

public class DefaultDispatcher implements Dispatcher {

	private final Logger logger = LoggerFactory.getLogger(DefaultDispatcher.class);
	private Map<String, Set<EventHandler>> eventHandlers = new ConcurrentHashMap<String, Set<EventHandler>>();

	@Override
	public void on(String event, Object handler, Method method) {
		on(event, new StaticEventHandler(handler, method));
	}
	
	@Override
	public void on(String event, Socket socket, Fn.Callback handler) {
		on(event, new DynamicEventHandler(socket, handler));
	}

	@Override
	public void on(String event, Socket socket, Fn.Callback1<?> handler) {
		on(event, new DynamicEventHandler(socket, handler));
	}

	@Override
	public void on(String event, Socket socket, Fn.Callback2<?, ?> handler) {
		on(event, new DynamicEventHandler(socket, handler));
	}
	
	private void on(String event, EventHandler eventHandler) {
		if (!eventHandlers.containsKey(event)) {
			eventHandlers.put(event, new CopyOnWriteArraySet<EventHandler>());
		}
		
		logger.debug("Attaching the {} event handler {}", event, eventHandler);
		try {
			eventHandlers.get(event).add(eventHandler.init());
		} catch (EventHandlerSignatureException e) {
			logger.error("Event handler method signature is inappropriate", e);
		}
	}
	
	@Override
	public void fire(String on, Socket socket) {
		fire(on, socket, null);
	}

	@Override
	public void fire(String on, Socket socket, Object data) {
		fire(on, socket, data, null);
	}

	@Override
	public void fire(String on, Socket socket, Object data, Fn.Callback1<Object> reply) {
		logger.info("Firing {} event to Socket#{}", on, socket.param("id"));
		if (eventHandlers.containsKey(on)) {
			for (EventHandler eventHandler : eventHandlers.get(on)) {
				logger.trace("Invoking handler {}", eventHandler);
				try {
					eventHandler.handle(socket, data, reply);
				} catch (Exception e) {
					logger.error("Exception occurred while invoking a handler " + eventHandler, e);
				}
			}
		}
	}
	
	public Map<String, Set<EventHandler>> eventHandlers() {
		return Collections.unmodifiableMap(eventHandlers);
	}

	@SuppressWarnings("serial")
	public static class EventHandlerSignatureException extends RuntimeException {

		public EventHandlerSignatureException(String msg) {
			super(msg);
		}

	}

	public static interface EventHandler {

		EventHandler init();

		EventHandler handle(Socket socket, Object data, Fn.Callback1<Object> reply) throws Exception;

	}

	static class StaticEventHandler implements EventHandler {

		ObjectMapper mapper = new ObjectMapper();
		Class<?> dataType;
		Class<?> replyType;
		
		Object handler;
		Method method;
		int length;
		int socketIndex = -1;
		int dataIndex = -1;
		int replyIndex = -1;

		StaticEventHandler(Object handler, Method method) {
			this.handler = handler;
			this.method = method;
			this.length = method.getParameterTypes().length;
		}

		@Override
		public EventHandler init() {
			Class<?>[] paramTypes = method.getParameterTypes();
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> paramType = paramTypes[i];
				if (Socket.class.equals(paramType)) {
					if (socketIndex > -1) {
						throw new EventHandlerSignatureException("Socket is duplicated in the parameters of " + method);
					}
					socketIndex = i;
				}
			}

			Annotation[][] paramAnnotations = method.getParameterAnnotations();
			for (int i = 0; i < paramAnnotations.length; i++) {
				Class<?> paramType = method.getParameterTypes()[i];
				for (Annotation annotation : paramAnnotations[i]) {
					if (Data.class.equals(annotation.annotationType())) {
						if (dataType != null) {
							throw new EventHandlerSignatureException("@Data is duplicated in the parameters of " + method);
						}
						dataIndex = i;
						dataType = paramType;
					}
					if (Reply.class.equals(annotation.annotationType())) {
						if (replyType != null) {
							throw new EventHandlerSignatureException("@Reply is duplicated in the parameters of " + method);
						}
						if (Fn.Callback.class.equals(paramType) || Fn.Callback1.class.equals(paramType)) {
							replyIndex = i;
							replyType = paramType;
						} else {
							throw new EventHandlerSignatureException("@Reply must be present either on Fn.Callback or Fn.Callback1 in " + method);
						}
					}
				}
			}

			int sum = socketIndex + dataIndex + replyIndex;
			if (length > 3 || (length == 3 && sum != 3) || (length == 2 && sum != 0) || (length == 1 && sum != -2) || (length == 0 && sum != -3)) {
				throw new EventHandlerSignatureException("There is an unhandled paramter in " + method);
			}
			
			return this;
		}

		@Override
		public EventHandler handle(Socket socket, Object data, final Fn.Callback1<Object> reply) throws Exception {
			Object[] args = new Object[length];
			
			if (socketIndex > -1) {
				args[socketIndex] = socket;
			}
			if (dataIndex > -1) {
				if (dataType != null) {
					data = mapper.convertValue(data, dataType);
				}
				args[dataIndex] = data;
			}
			if (replyType != null && reply != null && replyIndex > -1) {
				args[replyIndex] = Fn.Callback.class.equals(replyType) ? new Fn.Callback() {
					@Override
					public void call() {
						reply.call(null);
					}
				} : new Fn.Callback1<Object>() {
					@Override
					public void call(Object arg1) {
						reply.call(arg1);
					}
				};
			}

			method.invoke(handler, args);

			return this;
		}

	}

	static class DynamicEventHandler implements EventHandler {

		ObjectMapper mapper = new ObjectMapper();
		Class<?> dataType;
		Class<?> replyType;
		
		Socket socket;
		Fn.Callback handler;
		Fn.Callback1<?> handlerWithData;
		Fn.Callback2<?, ?> handlerWithDataAndReply;

		DynamicEventHandler(Socket socket, Fn.Callback handler) {
			this.socket = socket;
			this.handler = handler;
		}

		DynamicEventHandler(Socket socket, Fn.Callback1<?> handler) {
			this.socket = socket;
			this.handlerWithData = handler;
		}

		DynamicEventHandler(Socket socket, Fn.Callback2<?, ?> handler) {
			this.socket = socket;
			this.handlerWithDataAndReply = handler;
		}

		@Override
		public EventHandler init() {
			if (handlerWithData != null) {
				dataType = getDataType(handlerWithData);
			}
			if (handlerWithDataAndReply != null) {
				dataType = getDataType(handlerWithDataAndReply);
				replyType = getReplyType(handlerWithDataAndReply);

				if (!Fn.Callback.class.equals(replyType) && !Fn.Callback1.class.equals(replyType)) {
					throw new EventHandlerSignatureException("Reply functon must be either Fn.Callback or Fn.Callback1");
				}
			}
			return this;
		}

		// TODO enhance
		Class<?> getDataType(Object handler) {
			return (Class<?>) (((ParameterizedType) handler.getClass().getGenericInterfaces()[0]).getActualTypeArguments())[0];
		}

		// TODO enhance
		Class<?> getReplyType(Object handler) {
			Type type = (((ParameterizedType) handler.getClass().getGenericInterfaces()[0]).getActualTypeArguments())[1];
			// Fn.Callback vs Fn.Callback1<T>
			return (Class<?>) (type instanceof Class ? type : ((ParameterizedType) type).getRawType());
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public EventHandler handle(Socket socket, Object data, final Fn.Callback1<Object> reply) throws Exception {
			if (this.socket == socket) {
				if (dataType != null) {
					data = mapper.convertValue(data, dataType);
				}
				if (handler != null) {
					handler.call();
				} else if (handlerWithData != null) {
					((Fn.Callback1) handlerWithData).call(data);
				} else if (handlerWithDataAndReply != null) {
					((Fn.Callback2) handlerWithDataAndReply).call(data, Fn.Callback.class.equals(replyType) ? new Fn.Callback() {
						@Override
						public void call() {
							reply.call(null);
						}
					} : new Fn.Callback1<Object>() {
						@Override
						public void call(Object arg1) {
							reply.call(arg1);
						}
					});
				}
			}
			return this;
		}

	}

}
