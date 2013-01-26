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
package com.github.flowersinthesand.portal.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.spi.Dispatcher;

@Bean("com.github.flowersinthesand.portal.spi.Dispatcher")
public class DefaultDispatcher implements Dispatcher {

	private final Logger logger = LoggerFactory.getLogger(DefaultDispatcher.class);
	private Map<String, Set<Dispatcher.Handler>> handlers = new ConcurrentHashMap<String, Set<Dispatcher.Handler>>();

	@Override
	public Map<String, Set<Dispatcher.Handler>> handlers() {
		Map<String, Set<Dispatcher.Handler>> map = new LinkedHashMap<String, Set<Handler>>();
		for (Entry<String, Set<Handler>> entry : handlers.entrySet()) {
			map.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
		}
		
		return Collections.unmodifiableMap(map);
	}
	
	@Override
	public void on(String event, Object bean, Method method) {
		logger.debug("Attaching the '{}' event from '{}'", event, method);
		
		Dispatcher.Handler handler;
		try {
			handler = new DefaultHandler(bean, method);
		} catch (IllegalArgumentException e) {
			throw e;
		}
		
		if (!handlers.containsKey(event)) {
			handlers.put(event, new CopyOnWriteArraySet<Dispatcher.Handler>());
		}

		handlers.get(event).add(handler);
	}

	@Override
	public void fire(String event, Socket socket) {
		fire(event, socket, null);
	}

	@Override
	public void fire(String event, Socket socket, Object data) {
		fire(event, socket, data, null);
	}

	@Override
	public void fire(String event, Socket socket, Object data, Fn.Callback1<?> reply) {
		logger.info("Firing {} event to Socket#{}", event, socket.param("id"));
		if (handlers.containsKey(event)) {
			for (Dispatcher.Handler handler : handlers.get(event)) {
				logger.trace("Invoking handler {}", handler);
				try {
					handler.handle(socket, data, reply);
				} catch (Exception e) {
					logger.error("Exception occurred while invoking a handler " + handler, e);
				}
			}
		}
	}

	static class DefaultHandler implements Dispatcher.Handler {

		Logger logger = LoggerFactory.getLogger(DefaultHandler.class);
		ObjectMapper mapper = new ObjectMapper();
		Object bean;
		Method method;

		Class<?> dataType;
		Class<?> replyType;
		int length;
		int socketIndex = -1;
		int dataIndex = -1;
		int replyIndex = -1;
		boolean replied;

		DefaultHandler(Object bean, Method method) {
			this.bean = bean;
			this.method = method;
			this.length = method.getParameterTypes().length;
			
			Class<?>[] paramTypes = method.getParameterTypes();
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> paramType = paramTypes[i];
				if (Socket.class.equals(paramType)) {
					if (socketIndex > -1) {
						throw new IllegalArgumentException("Socket is duplicated in the parameters of " + method);
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
							throw new IllegalArgumentException("@Data is duplicated in the parameters of " + method);
						}
						dataIndex = i;
						dataType = paramType;
					}
					if (Reply.class.equals(annotation.annotationType())) {
						if (replyType != null) {
							throw new IllegalArgumentException("@Reply is duplicated in the parameters of " + method);
						}
						if (Fn.Callback.class.equals(paramType) || Fn.Callback1.class.equals(paramType)) {
							replyIndex = i;
							replyType = paramType;
						} else {
							throw new IllegalArgumentException("@Reply must be present either on Fn.Callback or Fn.Callback1 in " + method);
						}
					}
				}
			}

			int sum = socketIndex + dataIndex + replyIndex;
			if (length > 3 || (length == 3 && sum != 3) || (length == 2 && sum != 0) || (length == 1 && sum != -2) || (length == 0 && sum != -3)) {
				throw new IllegalArgumentException("There is an unhandled paramter in " + method);
			}
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void handle(Socket socket, Object data, final Fn.Callback1 reply) {
			Object[] args = new Object[length];
			Object result = null;
			
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
						replied = true;
						reply.call(null);
					}
				} : new Fn.Callback1<Object>() {
					@Override
					public void call(Object arg1) {
						replied = true;
						reply.call(arg1);
					}
				};
			}

			try {
				result = method.invoke(bean, args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if (reply != null && !replied && method.getReturnType() != Void.TYPE) {
				replied = true;
				reply.call(result);
			}
		}

	}

}
