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
package com.github.flowersinthesand.portal;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.spi.DefaultInitializer;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.Initializer;

import eu.infomas.annotation.AnnotationDetector;

public class App implements Serializable {

	private final static long serialVersionUID = 1728426808062835850L;
	private final static String FIRST = App.class.getName() + ".first";

	private static class RepositoryHolder {
		static final ConcurrentMap<String, App> defaults = new ConcurrentHashMap<String, App>();
	}

	private static ConcurrentMap<String, App> repository() {
		return RepositoryHolder.defaults;
	}

	public static App find() {
		return find(FIRST);
	}

	public static App find(String name) {
		return name == null ? null : repository().get(name);
	}

	private final Logger logger = LoggerFactory.getLogger(App.class);
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private String name;
	private Set<Initializer> initializers = new LinkedHashSet<Initializer>();
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();
	private Set<Object> handlers = new LinkedHashSet<Object>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();
	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();
	
	public App() {}

	public App init(String name) {
		return init(name, (Options) null);
	}

	public App init(String name, Options options) {
		return init(name, options, null);
	}

	public App init(String name, Initializer initializer) {
		return init(name, null, initializer);
	}

	public App init(String name, Options options, Initializer initializer) {
		this.name = name;
		initializers.addAll(loadInitializers(initializer));
		doInit(options != null ? options : new Options());
		return this;
	}

	protected Set<Initializer> loadInitializers(Initializer initializer) {
		Set<Initializer> initializers = new LinkedHashSet<Initializer>();

		initializers.add(new DefaultInitializer());
		for (Initializer i : ServiceLoader.load(Initializer.class)) {
			initializers.add(i);
		}
		if (initializer != null) {
			initializers.add(initializer);
		}
		
		return initializers;
	}
	
	protected void doInit(Options options) {
		if (initialized.get()) {
			throw new IllegalStateException("Already initialized");
		}
		logger.info("Initializing the app#{} with options {} and initializers {}", name, options, initializers);
		
		init(options);
		logger.info("Final options {}", options);

		beans.putAll(createBeans(options.classes()));
		logger.info("Instantiated beans {}", beans);
		for (Initializer p : initializers) {
			logger.trace("Invoking postBeanInstantiation of Initializer {}", p);
			for (Entry<String, Object> entry : beans.entrySet()) {
				p.postBeanInstantiation(entry.getKey(), entry.getValue());
			}
		}
		beans.putAll(options.beans());
		
		handlers.addAll(createHandlers(scan(options)));
		logger.info("Instantiated handlers {}", handlers);
		for (Initializer p : initializers) {
			logger.trace("Invoking postHandlerInstantiation of Initializer {}", p);
			for (Object handler : handlers) {
				p.postHandlerInstantiation(handler);
			}
		}

		initialized.set(true);
		for (Initializer p : initializers) {
			logger.trace("Invoking postInitialization of Initializer {}", p);
			p.postInitialization();
		}
		logger.info("Initializing the app#{} is completed", name);
	}

	protected void init(Options options) {
		for (Initializer p : initializers) {
			logger.trace("Invoking options of Initializer {}", p);
			p.init(this, options);
		}
	}

	protected Map<String, Object> createBeans(Map<String, Class<?>> classes) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		for (Initializer p : initializers) {
			logger.trace("Invoking instantiateBeans of Initializer {}", p);
			for (Entry<String, Class<?>> entry : classes.entrySet()) {
				Object bean = p.instantiateBean(entry.getKey(), entry.getValue());
				if (bean != null) {
					map.put(entry.getKey(), bean);
				}
			}
		}

		return map;
	}

	protected Set<Object> createHandlers(Set<Class<?>> classes) {
		Map<Class<?>, Object> map = new LinkedHashMap<Class<?>, Object>();

		for (Initializer p : initializers) {
			logger.trace("Invoking instantiateHandlers of Initializer {}", p);
			for (Class<?> clazz : classes) {
				Object handler = p.instantiateHandler(clazz);
				if (handler != null) {
					map.put(clazz, handler);
				}
			}
		}

		return new LinkedHashSet<Object>(map.values());
	}

	@SuppressWarnings("unchecked")
	protected Set<Class<?>> scan(Options options) {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final Set<Class<?>> annotations = new LinkedHashSet<Class<?>>(Arrays.asList(On.class, On.open.class, On.close.class, On.message.class));
		AnnotationDetector annotationDetector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {

			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { On.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				try {
					annotations.add(classLoader.loadClass(className));
				} catch (ClassNotFoundException e) {
					logger.error("Annotation " + className + " not found", e);
					throw new IllegalArgumentException(e);
				}
			}

		});
		for (String packageName : options.packages()) {
			logger.debug("Scanning an annotation annotated with @On in {}", packageName);

			try {
				annotationDetector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
				throw new IllegalStateException(e);
			}	
		}

		final Set<Class<?>> handlers = new LinkedHashSet<Class<?>>(options.handlers());
		AnnotationDetector handlerDetector = new AnnotationDetector(new AnnotationDetector.MethodReporter() {

			@Override
			public Class<? extends Annotation>[] annotations() {
				return annotations.toArray(new Class[] {});
			}

			@Override
			public void reportMethodAnnotation(Class<? extends Annotation> annotation, String className, String methodName) {
				try {
					handlers.add(classLoader.loadClass(className));
				} catch (ClassNotFoundException e) {
					logger.error("Handler class " + className + " not found", e);
					throw new IllegalArgumentException(e);
				}
			}

		});
		for (String packageName : options.packages()) {
			logger.debug("Scanning {} in {}", annotations, packageName);

			try {
				handlerDetector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
				throw new IllegalStateException(e);
			}
		}
		
		return handlers;
	}

	public App register() {
		return register(repository());
	}

	public App register(ConcurrentMap<String, App> repository) {
		if (!initialized.get()) {
			throw new IllegalStateException("Not initialized");
		}
		
		repository.putIfAbsent(FIRST, this);
		repository.put(name, this);
		
		return this;
	}

	public String name() {
		return name;
	}

	public Object get(String key) {
		return key == null ? null : attrs.get(key);
	}

	public App set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	public Map<String, Room> rooms() {
		return rooms;
	}

	public Room room(String name) {
		if (rooms.containsKey(name)) {
			return rooms.get(name);
		}
		rooms.put(name, new Room(name, rooms));
		return rooms.get(name);
	}

	public App fire(String event, Socket socket) {
		Dispatcher dispatcher = bean(Dispatcher.class);
		dispatcher.fire(event, socket);
		return this;
	}

	public App fire(String event, Socket socket, Object data) {
		Dispatcher dispatcher = bean(Dispatcher.class);
		dispatcher.fire(event, socket, data);
		return this;
	}

	public App fire(String event, Socket socket, Object data, Fn.Callback1<Object> reply) {
		Dispatcher dispatcher = bean(Dispatcher.class);
		dispatcher.fire(event, socket, data, reply);
		return this;
	}

	public Object bean(String name) {
		return beans.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(Class<? super T> clazz) {
		for (Object object : beans.values()) {
			if (clazz == object.getClass()) {
				return (T) object;
			}
		}
		for (Object object : beans.values()) {
			if (clazz.isAssignableFrom(object.getClass())) {
				return (T) object;
			}
		}
		return null;
	}

}
