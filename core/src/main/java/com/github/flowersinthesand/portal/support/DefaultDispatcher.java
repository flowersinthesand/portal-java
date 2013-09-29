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
package com.github.flowersinthesand.portal.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Order;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;

@Bean("dispatcher")
public class DefaultDispatcher implements Dispatcher {

	private final Logger logger = LoggerFactory.getLogger(DefaultDispatcher.class);
	private Map<String, SortedSet<Dispatcher.Handler>> handlers = new LinkedHashMap<String, SortedSet<Dispatcher.Handler>>();
	@Wire
	private Evaluator evaluator;

	@Override
	public Set<Dispatcher.Handler> handlers(String type) {
		return handlers.containsKey(type) ? Collections.unmodifiableSet(handlers.get(type)) : null;
	}

	@Override
	public void on(String type, Object bean, Method method) {
		logger.debug("Attaching the '{}' event from '{}'", type, method);
		
		Dispatcher.Handler handler;
		try {
			handler = new DefaultHandler(bean, method);
		} catch (IllegalArgumentException e) {
			throw e;
		}
		
		if (!handlers.containsKey(type)) {
			handlers.put(type, new TreeSet<Dispatcher.Handler>(new Comparator<Dispatcher.Handler>() {
				@Override
				public int compare(Handler o1, Handler o2) {
					return o1.order() > o2.order() ? 1 : -1;
				}
			}));
		}

		handlers.get(type).add(handler);
	}

	@Override
	public void fire(String type, Socket socket) {
		fire(type, socket, null);
	}

	@Override
	public void fire(String type, Socket socket, Object data) {
		fire(type, socket, data, 0);
	}

	@Override
	public void fire(String type, final Socket socket, Object data, final int eventIdForReply) {
		logger.debug("Firing {} event to Socket#{}", type, socket.id());
		Reply.Fn reply = eventIdForReply > 0 ? new Reply.Fn() {
			@Override
			public void done() {
				done(null);
			}

			@Override
			public void done(Object data) {
				Map<String, Object> result = new LinkedHashMap<String, Object>();
				result.put("id", eventIdForReply);
				result.put("data", data);
				result.put("exception", false);

				logger.debug("Sending the reply event with the data {}", data);
				socket.send("reply", result);
			}

			@Override
			public void fail(Throwable error) {
				Map<String, Object> data = new LinkedHashMap<String, Object>();
				data.put("type", error.getClass().getName());
				data.put("message", error.getMessage());

				Map<String, Object> result = new LinkedHashMap<String, Object>();
				result.put("id", eventIdForReply);
				result.put("data", data);
				result.put("exception", true);

				logger.debug("Sending the reply event with the error {}", error);
				socket.send("reply", result);
			}
		} : null;
				
		if (handlers.containsKey(type)) {
			for (Dispatcher.Handler handler : handlers.get(type)) {
				logger.trace("Invoking handler {}", handler);
				handler.handle(socket, data, reply);
			}
		}
	}

	@SuppressWarnings("unchecked")
	class DefaultHandler implements Dispatcher.Handler {

		int order = 0;
		ObjectMapper mapper = new ObjectMapper();
		Object bean;
		Method method;
		Param[] params;
		boolean replyOnMethod;
		Class<?>[] throwables;
		
		DefaultHandler(Object bean, Method method) {
			this.bean = bean;
			this.method = method;
			
			if (method.isAnnotationPresent(Order.class)) {
				this.order = method.getAnnotation(Order.class).value();
			}
			
			replyOnMethod = method.isAnnotationPresent(Reply.class);
			if (replyOnMethod) {
				throwables = method.getAnnotation(Reply.class).failFor();
				if (throwables.length == 0) {
					throwables = method.getExceptionTypes();
				}
			}
			
			Class<?>[] paramTypes = method.getParameterTypes();
			Annotation[][] paramAnnotations = method.getParameterAnnotations();
			params = new Param[paramTypes.length];
			
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> paramType = paramTypes[i];
				if (paramType.isAssignableFrom(Socket.class)) {
					params[i] = new SocketParam();					
				}
				for (Annotation annotation : paramAnnotations[i]) {
					if (Data.class.equals(annotation.annotationType())) {
						params[i] = new DataParam(paramType, (Data) annotation);
					}
					if (Reply.class.equals(annotation.annotationType())) {
						if (replyOnMethod) {
							throw new IllegalArgumentException("@Reply is already annotated to the method '" + method + "'");
						}
						if (!Reply.Fn.class.equals(paramType)) {
							throw new IllegalArgumentException(
								"@Reply must be present Reply.Fn not '" + paramType + "' in '" + method + "'");
						}
						if (((Reply) annotation).failFor().length != 0) {
							throw new IllegalArgumentException(
								"@Reply annotated to the parameter '" + paramType + "' in '" + method + "' cannot have fail attribute");
						}
						params[i] = new ReplyParam();
					}
				}
			}
			
			int socketIndex = -1;
			int replyIndex = -1;
			List<String> expressions = new ArrayList<String>();
			for (int i = 0; i < params.length; i++) {
				Param arg = params[i];
				if (arg == null) {
					throw new IllegalArgumentException(
						"Paramters[" + i + "] '" + method.getParameterTypes()[i] + "' of method '" + method + "' cannot be resolved");
				} else if (arg instanceof SocketParam) {
					if (socketIndex != -1) {
						throw new IllegalArgumentException(
							"Socket is duplicated at '" + socketIndex + "' and '" + i + "' index in parameters of '" + method + "'");
					}
					socketIndex = i;
				} else if (arg instanceof DataParam) {
					String value = ((DataParam) arg).ann.value();
					if (expressions.contains(value)) {
						throw new IllegalArgumentException("@Data(\"" + value + "\") is duplicated in paramters of '" + method + "'");
					}
					expressions.add(value);
				} else if (arg instanceof ReplyParam) {
					if (replyIndex != -1) {
						throw new IllegalArgumentException(
							"@Reply is duplicated at '" + replyIndex + "' and '" + i + "' index in parameters of '" + method + "'");
					}
					if (method.isAnnotationPresent(Reply.class)) {
						throw new IllegalArgumentException("@Reply is alread annotated to the method '" + method + "'");
					}
				}
			}
		}

		@Override
		public int order() {
			return order;
		}

		@Override
		public void handle(Socket socket, Object data, Reply.Fn reply) {
			Object[] args = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				args[i] = params[i].resolve(socket, data, reply);
			}

			try {
				Object result = method.invoke(bean, args);
				if (replyOnMethod) {
					reply.done(result);
				}
			} catch (InvocationTargetException e) {
				boolean handled = false;
				Throwable ex = e.getCause();
				
				if (throwables != null) {
					for (Class<?> throwable : throwables) {
						if (ex.getClass().isAssignableFrom(throwable)) {
							reply.fail(ex);
							handled = true;
							break;
						}
					}
				}
				if (!handled) {
					throw new RuntimeException(e);
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
		abstract class Param {
			abstract Object resolve(Socket socket, Object data, Reply.Fn reply);
		}
		
		class SocketParam extends Param {
			@Override
			Object resolve(Socket socket, Object data, Reply.Fn reply) {
				return socket;
			}
		}

		class DataParam extends Param {
			Class<?> type;
			Data ann;

			public DataParam(Class<?> type, Data ann) {
				this.type = type;
				this.ann = ann;
			}

			@Override
			Object resolve(Socket socket, Object data, Reply.Fn reply) {
				if (!ann.value().equals("")) {
					if (!(data instanceof Map)) {
						throw new IllegalArgumentException("@Data(\"" + ann.value() + "\") must work with Map not '" + data + "'");
					}
					data = evaluator.evaluate((Map<String, Object>) data, ann.value());
				}
				
				return mapper.convertValue(data, type);
			}
		}

		class ReplyParam extends Param {
			@Override
			Object resolve(Socket socket, Object data, final Reply.Fn reply) {
				return reply;
			}
		}

	}

	@Bean("dispatcher.Evaluator")
	public static class DefaultEvaluator implements Dispatcher.Evaluator {

		@Override
		public Object evaluate(Map<String, Object> root, String expression) {
			return root.get(expression);
		}

	}

}
