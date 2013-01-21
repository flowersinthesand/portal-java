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
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();
	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();
	
	public App() {}

	public App init(Options options) {
		return init(options, null);
	}

	public App init(Options options, Initializer initializer) {
		if (initialized.get()) {
			throw new IllegalStateException("Already initialized");
		}
		if (options.url() == null) {
			throw new IllegalArgumentException("Option's url cannot be null");
		}
		
		setName(options);
		doInit(options, loadInitializers(initializer));

		return this;
	}
	
	public String name() {
		return name;
	}

	protected void setName(Options options) {
		this.name = options.name() != null ? options.name() : options.url();
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

	protected void doInit(Options options, Set<Initializer> initializers) {
		logger.info("Initializing the Portal application with options {} and initializers {}", options, initializers);

		for (Initializer i : initializers) {
			i.init(this, options);
		}
		logger.info("Final options {}", options);
		
		Map<String, Class<?>> classes = scan(options.packages());
		for (Initializer i : initializers) {
			for (Entry<String, Class<?>> entry : classes.entrySet()) {
				Object bean = i.instantiate(entry.getKey(), entry.getValue());
				if (bean != null) {
					beans.put(entry.getKey(), bean);
				}
			}
		}
		beans.putAll(options.beans());
		for (Initializer i : initializers) {
			for (Entry<String, Object> entry : beans.entrySet()) {
				i.postInstantiation(entry.getKey(), entry.getValue());
			}
		}
		logger.info("Final beans {}", beans);
		
		initialized.set(true);
		logger.info("Initializing the application {} is completed", this);
		
		for (Initializer i : initializers) {
			i.postInitialization();
		}
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Class<?>> scan(Set<String> packages) {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final Map<String, Class<?>> classes = new LinkedHashMap<String, Class<?>>();
		AnnotationDetector detector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {

			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { Bean.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				Class<?> clazz;
				try {
					clazz = classLoader.loadClass(className);
				} catch (ClassNotFoundException e) {
					logger.error("Bean " + className + " not found", e);
					throw new IllegalArgumentException(e);
				}
				
				String name = clazz.getAnnotation(Bean.class).value();
				logger.debug("Scanned @Bean(\"{}\") on {}", name, className);
				
				if (name.length() == 0) {
					name = className;
				}
				
				classes.put(name, clazz);
			}
		});

		for (String packageName : packages) {
			logger.debug("Scanning @Bean in {}", packageName);

			try {
				detector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
				throw new IllegalStateException(e);
			}
		}
		
		return classes;
	}

	public App register() {
		if (!initialized.get()) {
			throw new IllegalStateException("Not initialized");
		}
		
		repository().putIfAbsent(FIRST, this);
		repository().put(name, this);
		
		return this;
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
		Dispatcher dispatcher = (Dispatcher) bean(Dispatcher.class.getName());
		dispatcher.fire(event, socket);
		return this;
	}

	public App fire(String event, Socket socket, Object data) {
		Dispatcher dispatcher = (Dispatcher) bean(Dispatcher.class.getName());
		dispatcher.fire(event, socket, data);
		return this;
	}

	public App fire(String event, Socket socket, Object data, Fn.Callback1<Object> reply) {
		Dispatcher dispatcher = (Dispatcher) bean(Dispatcher.class.getName());
		dispatcher.fire(event, socket, data, reply);
		return this;
	}

	public Object bean(String name) {
		return beans.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(Class<T> clazz) {
		Set<String> names = new LinkedHashSet<String>();

		for (Entry<String, Object> entry : beans.entrySet()) {
			if (clazz == entry.getValue().getClass()) {
				names.add(entry.getKey());
			}
		}

		if (names.isEmpty()) {
			for (Entry<String, Object> entry : beans.entrySet()) {
				if (clazz.isAssignableFrom(entry.getValue().getClass())) {
					names.add(entry.getKey());
				}
			}
		}

		if (names.size() > 1) {
			throw new IllegalArgumentException("Multiple beans found " + names + " for " + clazz);
		}

		return names.isEmpty() ? null : (T) bean(names.iterator().next());
	}

}
