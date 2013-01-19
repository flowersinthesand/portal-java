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
	private Set<Initializer> initializers = new LinkedHashSet<Initializer>();
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();
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
		
		beans.putAll(createBeans(scan(options.packages())));
		logger.info("Created beans {}", beans);
		for (Initializer i : initializers) {
			logger.trace("Invoking postBeanInstantiation of Initializer {}", i);
			for (Entry<String, Object> entry : beans.entrySet()) {
				i.postInstantiation(entry.getKey(), entry.getValue());
			}
		}
		beans.putAll(options.beans());
		
		initialized.set(true);
		for (Initializer i : initializers) {
			logger.trace("Invoking postInitialization of Initializer {}", i);
			i.postInitialization();
		}
		logger.info("Initializing the app#{} is completed", name);
	}

	protected void init(Options options) {
		for (Initializer i : initializers) {
			logger.trace("Invoking options of Initializer {}", i);
			i.init(this, options);
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
				if (name.length() == 0) {
					name = clazz.getName();
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

	protected Map<String, Object> createBeans(Map<String, Class<?>> classes) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		for (Initializer i : initializers) {
			logger.trace("Invoking instantiateBeans of Initializer {}", i);
			for (Entry<String, Class<?>> entry : classes.entrySet()) {
				Object bean = i.instantiate(entry.getKey(), entry.getValue());
				if (bean != null) {
					map.put(entry.getKey(), bean);
				}
			}
		}

		return map;
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
