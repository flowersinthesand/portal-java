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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Map.Entry;
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
	private Options options;
	private Set<Initializer> initializers;
	private Map<Class<?>, Object> beans;
	private Set<Object> handlers;
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
		this.options = options != null ? options : new Options();
		loadInitializers(initializer);
		doInit();
		return this;
	}

	protected void loadInitializers(Initializer initializer) {
		initializers = new LinkedHashSet<Initializer>();
		initializers.add(new DefaultInitializer());
		for (Initializer i : ServiceLoader.load(Initializer.class)) {
			initializers.add(i);
		}
		if (initializer != null) {
			initializers.add(initializer);
		}
	}
	
	protected void doInit() {
		if (initialized.get()) {
			throw new IllegalStateException("Already initialized");
		}
		logger.info("Initializing the app#{} with options {} and initializers {}", name, options, initializers);

		options = resolveOptions(Collections.unmodifiableMap(options.props())).merge(options);
		logger.info("Resovled options {}", options);

		beans = Collections.unmodifiableMap(createBeans(options.classes()));
		logger.info("Instantiated beans {}", beans);
		
		for (Initializer p : initializers) {
			logger.trace("Invoking postBeansInstantiation of Initializer {}", p);
			p.postBeansInstantiation(beans);
		}

		handlers = Collections.unmodifiableSet(createHandlers(scan(options)));
		logger.info("Instantiated handlers {}", handlers);
		
		for (Initializer p : initializers) {
			logger.trace("Invoking postHandlersInstantiation of Initializer {}", p);
			p.postHandlersInstantiation(handlers);
		}

		initialized.set(true);
		logger.info("Initializing the app#{} is completed", name);
	}

	protected Options resolveOptions(Map<String, Object> properties) {
		Options options = new Options();

		for (Initializer p : initializers) {
			logger.trace("Invoking options of Initializer {}", p);
			options.merge(p.init(this, properties));
		}

		return options;
	}

	protected Map<Class<?>, Object> createBeans(Map<Class<?>, Class<?>> classes) {
		Map<Class<?>, Object> map = new LinkedHashMap<Class<?>, Object>();

		for (Initializer p : initializers) {
			logger.trace("Invoking instantiateBeans of Initializer {}", p);
			for (Entry<Class<?>, Class<?>> entry : classes.entrySet()) {
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

	protected Set<Class<?>> scan(Options options) {
		
		String base = "";
		if (options.base() != null) {
			try {
				base = new File(options.base()).getCanonicalPath();
			} catch (IOException e) {
				logger.error("Cannot resolve the canonical path of the base " + options.base(), e);
			}
		}

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final Set<Class<?>> annotations = new LinkedHashSet<Class<?>>();
		annotations.add(On.class);
		annotations.add(On.open.class);
		annotations.add(On.close.class);
		annotations.add(On.message.class);
		AnnotationDetector annotationDetector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {

			@SuppressWarnings("unchecked")
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
				}
			}

		});
		for (String location : options.locations()) {
			location = base + ((location.length() != 0 && location.charAt(0) != '/') ? "/" : "") + location;
			logger.debug("Scanning an annotation annotated with @On in {}", location);

			try {
				annotationDetector.detect(new File(location));
			} catch (IOException e) {
				logger.error("Failed to scan in " + location, e);
			}
		}
		for (String packageName : options.packages()) {
			logger.debug("Scanning an annotation annotated with @On in {}", packageName);

			try {
				annotationDetector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
			}	
		}

		final Set<Class<?>> handlers = new LinkedHashSet<Class<?>>(options.handlers());
		AnnotationDetector handlerDetector = new AnnotationDetector(new AnnotationDetector.MethodReporter() {

			@SuppressWarnings("unchecked")
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
				}
			}

		});
		
		for (String location : options.locations()) {
			location = base + ((location.length() != 0 && location.charAt(0) != '/') ? "/" : "") + location;
			logger.debug("Scanning {} in {}", annotations, location);

			try {
				handlerDetector.detect(new File(location));
			} catch (IOException e) {
				logger.error("Failed to scan in " + location, e);
			}
		}
		for (String packageName : options.packages()) {
			logger.debug("Scanning {} in {}", annotations, packageName);

			try {
				handlerDetector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
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
		Dispatcher dispatcher = unwrap(Dispatcher.class);
		dispatcher.fire(event, socket);
		return this;
	}

	public App fire(String event, Socket socket, Object data) {
		Dispatcher dispatcher = unwrap(Dispatcher.class);
		dispatcher.fire(event, socket, data);
		return this;
	}

	public App fire(String event, Socket socket, Object data, Fn.Callback1<Object> reply) {
		Dispatcher dispatcher = unwrap(Dispatcher.class);
		dispatcher.fire(event, socket, data, reply);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<? super T> clazz) {
		return (T) beans.get(clazz);
	}

}
