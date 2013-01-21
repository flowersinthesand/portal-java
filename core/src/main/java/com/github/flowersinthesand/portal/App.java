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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.Initializer;
import com.github.flowersinthesand.portal.spi.NewObjectFactory;
import com.github.flowersinthesand.portal.spi.ObjectFactory;

import eu.infomas.annotation.AnnotationDetector;

public final class App {

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
	private String name;
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();
	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();
	
	public App() {}

	public App(Options options) {
		init(options);
	}

	public void init(Options options) {
		if (options.url() == null) {
			throw new IllegalArgumentException("Option's url cannot be null");
		}

		this.name = options.name();
		Set<Initializer> initializers = loadInitializers();
		logger.info("Initializing Portal application with options {} and initializers {}", options, initializers);

		for (Initializer i : initializers) {
			i.init(options);
		}
		logger.info("Final options {}", options);

		Map<String, Class<?>> classes = scan(options.packages("com.github.flowersinthesand.portal").packages());
		beans.putAll(options.beans());
		
		if (!beans.containsKey(ObjectFactory.class.getName())) {
			beans.put(ObjectFactory.class.getName(), new NewObjectFactory());
		}
		ObjectFactory factory = (ObjectFactory) beans.get(ObjectFactory.class.getName());
		logger.info("ObjectFactory '{}' is prepared", factory);

		for (Entry<String, Class<?>> entry : classes.entrySet()) {
			Object bean = factory.create(entry.getKey(), entry.getValue());
			logger.debug("Bean '{}' is instantiated '{}'", entry.getKey(), bean);
			beans.put(entry.getKey(), bean);
		}
		for (Entry<String, Object> entry : beans.entrySet()) {
			logger.debug("Processing bean '{}'", entry.getKey());
			Object bean = entry.getValue();
			
			for (Field field : bean.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(Wire.class)) {
					wire(bean, field);
				}
			}
			for (Method method : bean.getClass().getMethods()) {
				onIfPossible(bean, method);
				if (method.isAnnotationPresent(Prepare.class)) {
					prepare(bean, method);
				}
			}
		}
	}

	private Set<Initializer> loadInitializers() {
		Set<Initializer> initializers = new LinkedHashSet<Initializer>();

		for (Initializer i : ServiceLoader.load(Initializer.class)) {
			initializers.add(i);
		}

		return initializers;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Class<?>> scan(Set<String> packages) {
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
				logger.debug("Scanned @Bean(\"{}\") on '{}'", name, className);
				
				if (name.length() == 0) {
					name = className;
				}
				
				classes.put(name, clazz);
			}
		});

		for (String packageName : packages) {
			logger.debug("Scanning the package '{}'", packageName);

			try {
				detector.detect(packageName);
			} catch (IOException e) {
				logger.error("Failed to scan in " + packageName, e);
				throw new IllegalStateException(e);
			}
		}
		
		return classes;
	}
	
	private void wire(Object bean, Field field) {
		String beanName = field.getAnnotation(Wire.class).value();
		Class<?> beanType = field.getType();
		logger.debug("@Wire(\"{}\") on '{}'", beanName, field);

		Object value = null;
		if (beanType.isAssignableFrom(App.class)) {
			value = this;
		} else if (beanType.isAssignableFrom(Room.class)) {
			if (beanName.length() == 0) {
				throw new IllegalArgumentException("Room has no name in @Wire(\"\") " + field);
			}
			value = room(beanName);
		} else {
			value = beanName.length() > 0 ? bean(beanName) : bean(beanType);
		}
		
		if (value == null) {
			throw new IllegalStateException("No value to wire the field @Wire(\"" + beanName + "\")" + field);
		}

		try {
			field.setAccessible(true);
			field.set(bean, value);
		} catch (Exception e) {
			logger.error("Failed to set " + field + " to " + value, e);
			throw new RuntimeException(e);
		}
	}

	private void onIfPossible(Object bean, Method method) {
		String on = null;
		if (method.isAnnotationPresent(On.class)) {
			on = method.getAnnotation(On.class).value();
			logger.debug("@On(\"{}\") on '{}'", on, method);
		} else {
			for (Annotation ann : method.getAnnotations()) {
				if (ann.annotationType().isAnnotationPresent(On.class)) {
					on = ann.annotationType().getAnnotation(On.class).value();
					logger.debug("@On(\"{}\") of '{}' on '{}'", on, ann, method);
					break;
				}
			}
		}
		
		if (on != null) {
			Dispatcher dispatcher = (Dispatcher) bean(Dispatcher.class.getName());
			dispatcher.on(on, bean, method);
		}
	}

	private void prepare(Object bean, Method method) {
		logger.debug("@Prepare on '{}'", method);
		try {
			method.invoke(bean);
		} catch (Exception e) {
			logger.error("Failed to execute @Prepare method " + method, e);
			throw new RuntimeException(e);
		}
	}
	
	public App register() {
		repository().putIfAbsent(FIRST, this);
		repository().put(name, this);
		
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
