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
package com.github.flowersinthesand.portal;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.Module;
import com.github.flowersinthesand.portal.spi.ObjectFactory;
import com.github.flowersinthesand.portal.spi.RoomFactory;
import com.github.flowersinthesand.portal.support.NewObjectFactory;

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

	public static String version() {
		return App.class.getPackage().getImplementationVersion();
	}

	private final Logger logger = LoggerFactory.getLogger(App.class);
	private String name;
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();
	
	public App(Options options, Module... modules) {
		init(options, Arrays.asList(modules));
	}

	private void init(Options options, List<Module> modules) {
		if (options.url() == null) {
			throw new IllegalArgumentException("Option's url cannot be null");
		}

		this.name = options.name();
		logger.info("Initializing Portal application with options {} and modules {}", options, modules);

		for (Module module : modules) {
			logger.trace("Configuring the module '{}'", module);
			module.configure(options.packageOf(module));
		}
		
		options.packageOf("com.github.flowersinthesand.portal.support");
		logger.info("Final options {}", options);

		List<String> packages = new ArrayList<String>(options.packages());
		Collections.reverse(packages);
		
		Map<String, Class<?>> classes = scan(packages);
		beans.putAll(options.beans());
		
		if (!beans.containsKey(ObjectFactory.class.getName())) {
			beans.put(ObjectFactory.class.getName(), new NewObjectFactory());
		}
		ObjectFactory factory = (ObjectFactory) beans.get(ObjectFactory.class.getName());
		logger.info("ObjectFactory '{}' is initialized", factory);

		for (Entry<String, Class<?>> entry : classes.entrySet()) {
			Object bean = factory.create(entry.getKey(), entry.getValue());
			logger.trace("Bean '{}' is instantiated '{}'", entry.getKey(), bean);
			beans.put(entry.getKey(), bean);
		}

		for (Entry<String, Object> entry : beans.entrySet()) {
			logger.debug("Processing bean '{}'", entry.getKey());
			
			Object bean = entry.getValue();
			List<Field> fields = getAllDeclaredFields(bean.getClass());
			List<Method> methods = getAllDeclaredMethods(bean.getClass());

			for (Field field : fields) {
				if (field.isAnnotationPresent(Wire.class)) {
					wireBean(bean, field);
				}
			}
			for (Method method : methods) {
				if (method.isAnnotationPresent(On.class)) {
					registerEventHandler(bean, method);
				}
			}
			for (Method method : methods) {
				if (method.isAnnotationPresent(Init.class)) {
					initBean(bean, method);
				}
			}
		}
	}

	private List<Field> getAllDeclaredFields(Class<?> targetClass) {
		List<Field> fields = new ArrayList<Field>();
		while (targetClass != null) {
			for (Field field : targetClass.getDeclaredFields()) {
				fields.add(field);
			}
			targetClass = targetClass.getSuperclass();
		}

		return fields;
	}

	private List<Method> getAllDeclaredMethods(Class<?> targetClass) {
		List<Method> methods = new ArrayList<Method>();
		while (targetClass != null) {
			for (Method method : targetClass.getDeclaredMethods()) {
				methods.add(method);
			}
			targetClass = targetClass.getSuperclass();
		}

		return methods;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Class<?>> scan(List<String> packages) {
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
					logger.error("Bean '" + className + "' not found", e);
					throw new IllegalArgumentException(e);
				}
				
				String name = clazz.getAnnotation(Bean.class).value();
				if (name.length() == 0) {
					name = Introspector.decapitalize(className.substring(className.lastIndexOf('.') + 1).replace('$', '.'));
				}
				
				classes.put(name, clazz);
				logger.debug("Scanned @Bean(\"{}\") on '{}'", name, className);
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
	
	private void wireBean(Object bean, Field field) {
		String beanName = field.getAnnotation(Wire.class).value();
		boolean specified = beanName.length() > 0; 
		Class<?> beanType = field.getType();
		if (beanName.length() == 0) {
			beanName = field.getName();
		}
		
		Object value = null;
		if (beanType.isAssignableFrom(App.class)) {
			value = this;
		} else if (beanType.isAssignableFrom(Room.class)) {
			value = room(beanName);
		} else {
			try {
				value = bean(beanName, beanType);
			} catch (IllegalArgumentException e) {
				if (specified) {
					throw e;
				} else {
					value = bean(beanType);
				}
			}
		}

		logger.debug("Wiring '{}' to '{}'", value, field);
		try {
			field.setAccessible(true);
			field.set(bean, value);
		} catch (Exception e) {
			logger.error("Failed to set " + field + " to " + value, e);
			throw new RuntimeException(e);
		}
	}

	private void registerEventHandler(Object bean, Method method) {
		String on = method.getAnnotation(On.class).value();
		if (on.length() == 0) {
			on = method.getName();
		}
		
		logger.debug("Registering '{}' event handler from ''", on, method);
		bean(Dispatcher.class).on(on, bean, method);
	}

	private void initBean(Object bean, Method method) {
		logger.debug("Executing init method '{}'", method);
		try {
			method.invoke(bean);
		} catch (Exception e) {
			logger.error("Failed to execute @Init method " + method, e);
			throw new RuntimeException(e);
		}
	}

	public void close() {
		logger.info("Closing Portal application");
		for (Entry<String, Object> entry : beans.entrySet()) {
			Object bean = entry.getValue();

			Class<?> targetClass = bean.getClass();
			while (targetClass != null) {
				for (Method method : targetClass.getDeclaredMethods()) {
					if (method.isAnnotationPresent(Destroy.class)) {
						destroyBean(bean, method);
					}
				}
				targetClass = targetClass.getSuperclass();
			}
		}

		beans.clear();
		attrs.clear();
	}

	private void destroyBean(Object bean, Method method) {
		logger.debug("Executing destroy method '{}'", method);
		try {
			method.invoke(bean);
		} catch (Exception e) {
			logger.error("Failed to execute @Destroy method " + method, e);
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

	public Object bean(String name) {
		if (!beans.containsKey(name)) {
			throw new IllegalArgumentException("Bean '" + name + "' not found");
		}

		return beans.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(String name, Class<T> clazz) {
		Object bean = bean(name);
		if (bean != null && !bean.getClass().isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Bean '" + name + "' is found, but its type '"
					+ bean.getClass() + "' is different comparing to the given one '" + clazz + "'");
		}

		return (T) bean;
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(Class<T> clazz) {
		Set<String> names = new LinkedHashSet<String>();

		for (Entry<String, Object> entry : beans.entrySet()) {
			if (clazz.isAssignableFrom(entry.getValue().getClass())) {
				names.add(entry.getKey());
			}
		}

		if (names.size() > 1) {
			throw new IllegalArgumentException("Multiple beans found " + names + " for " + clazz);
		} else if (names.isEmpty()) {
			throw new IllegalArgumentException("No bean found for " + clazz);
		}

		return (T) bean(names.iterator().next());
	}

	public Room room(String name) {
		RoomFactory factory = bean(RoomFactory.class);
		Room room = factory.find(name);
		if (room == null) {
			room = factory.open(name);
		}

		return room;
	}

	public Room hall() {
		return room("hall");
	}

}
