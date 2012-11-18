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
package org.flowersinthesand.portal.dispatcher;

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
import org.flowersinthesand.portal.Data;
import org.flowersinthesand.portal.Fn;
import org.flowersinthesand.portal.Reply;
import org.flowersinthesand.portal.Socket;
import org.flowersinthesand.portal.config.InitializerContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher {

	private final Logger logger = LoggerFactory.getLogger(InitializerContextListener.class);
	private Map<String, Map<String, Set<Invoker>>> events = new ConcurrentHashMap<String, Map<String, Set<Invoker>>>();

	public Map<String, Map<String, Set<Invoker>>> events() {
		return Collections.unmodifiableMap(events);
	}

	public void on(String handler, String on, Object instance, Method method) {
		on(handler, on, new StaticInvoker(instance, method).init());
	}
	
	public void on(String handler, String on, Socket socket, Fn.Callback instance) {
		on(handler, on, new DynamicInvoker(socket, instance).init());
	}

	public void on(String handler, String on, Socket socket, Fn.Callback1<?> instance) {
		on(handler, on, new DynamicInvoker(socket, instance).init());
	}

	public void on(String handler, String on, Socket socket, Fn.Callback2<?, ?> instance) {
		on(handler, on, new DynamicInvoker(socket, instance).init());
	}
	
	private void on(String handler, String on, Invoker invoker) {
		if (!events.containsKey(handler)) {
			events.put(handler, new ConcurrentHashMap<String, Set<Invoker>>());
		}
		if (!events.get(handler).containsKey(on)) {
			events.get(handler).put(on, new CopyOnWriteArraySet<Invoker>());
		}

		events.get(handler).get(on).add(invoker);
	}
	
	public void fire(String handler, String on, Socket socket) {
		fire(handler, on, socket, null);
	}

	public void fire(String handler, String on, Socket socket, Object data) {
		fire(handler, on, socket, data, null);
	}

	public void fire(String handler, String on, Socket socket, Object data, Fn.Callback1<Object> reply) {
		if (events.containsKey(handler)) {
			if (events.get(handler).containsKey(on)) {
				for (Invoker invoker : events.get(handler).get(on)) {
					try {
						invoker.invoke(socket, data, reply);
					} catch (Exception e) {
						// TODO
						logger.warn("", e);
					}
				}
			}
		}
	}

	public static interface Invoker {

		Invoker init();

		Invoker invoke(Socket socket, Object data, Fn.Callback1<Object> reply) throws Exception;

	}

	static class StaticInvoker implements Invoker {

		ObjectMapper mapper = new ObjectMapper();
		Class<?> dataType;
		Class<?> replyType;
		
		Object handler;
		Method method;
		int length;
		int socketIndex = -1;
		int dataIndex = -1;
		int replyIndex = -1;

		StaticInvoker(Object handler, Method method) {
			this.handler = handler;
			this.method = method;
			this.length = method.getParameterTypes().length;
		}

		@Override
		public Invoker init() {
			Class<?>[] paramTypes = method.getParameterTypes();
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> paramType = paramTypes[i];
				if (Socket.class.equals(paramType)) {
					if (socketIndex > -1) {
						throw new RuntimeException("duplicated Socket");
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
							throw new RuntimeException("duplicated @Data");
						}
						dataIndex = i;
						dataType = paramType;
					}
					if (Reply.class.equals(annotation.annotationType())) {
						if (replyType != null) {
							throw new RuntimeException("duplicated @Reply");
						}
						if (Fn.Callback.class.equals(paramType)
								|| Fn.Callback1.class.equals(paramType)) {
							replyIndex = i;
							replyType = paramType;
						} else {
							throw new RuntimeException("wrong");
						}
					}
				}
			}

			int sum = socketIndex + dataIndex + replyIndex;
			switch (length) {
			case 3:
				if (sum != 3) {
					throw new RuntimeException("wrong");
				}
				break;
			case 2:
				if (sum != 0) {
					throw new RuntimeException("wrong");
				}
				break;
			case 1:
				if (sum != -2) {
					throw new RuntimeException("wrong");
				}
				break;
			case 0:
				if (sum != -3) {
					throw new RuntimeException("wrong");
				}
				break;
			default:
				throw new RuntimeException("wrong");
			}

			return this;
		}

		@Override
		public Invoker invoke(Socket socket, Object data, final Fn.Callback1<Object> reply) throws Exception {
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

	static class DynamicInvoker implements Invoker {

		ObjectMapper mapper = new ObjectMapper();
		Class<?> dataType;
		Class<?> replyType;
		
		Socket socket;
		Fn.Callback handler;
		Fn.Callback1<?> handlerWithData;
		Fn.Callback2<?, ?> handlerWithDataAndReply;

		DynamicInvoker(Socket socket, Fn.Callback handler) {
			this.socket = socket;
			this.handler = handler;
		}

		DynamicInvoker(Socket socket, Fn.Callback1<?> handler) {
			this.socket = socket;
			this.handlerWithData = handler;
		}

		DynamicInvoker(Socket socket, Fn.Callback2<?, ?> handler) {
			this.socket = socket;
			this.handlerWithDataAndReply = handler;
		}

		@Override
		public Invoker init() {
			if (handlerWithData != null) {
				dataType = getDataType(handlerWithData);
			}
			if (handlerWithDataAndReply != null) {
				dataType = getDataType(handlerWithDataAndReply);
				replyType = getReplyType(handlerWithDataAndReply);

				if (!Fn.Callback.class.equals(replyType) && !Fn.Callback1.class.equals(replyType)) {
					throw new RuntimeException("wrong");
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
		public Invoker invoke(Socket socket, Object data, final Fn.Callback1<Object> reply) throws Exception {
			if (this.socket == socket) {
				if (dataType != null) {
					data = mapper.convertValue(data, dataType);
				}
				if (handler != null) {
					handler.call();
				}
				if (handlerWithData != null) {
					((Fn.Callback1) handlerWithData).call(data);
				}
				if (handlerWithDataAndReply != null) {
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
