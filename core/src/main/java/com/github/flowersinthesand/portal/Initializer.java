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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.spi.DefaultDispatcher;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.NoOpSocketManager;
import com.github.flowersinthesand.portal.spi.SocketManager;

import eu.infomas.annotation.AnnotationDetector;

public class Initializer {

	private final Logger logger = LoggerFactory.getLogger(Initializer.class);
	private Map<String, App> apps = new LinkedHashMap<String, App>();
	private Set<Preparer> preparers = new LinkedHashSet<Preparer>();
	private Options options = new Options().classes(SocketManager.class, NoOpSocketManager.class, Dispatcher.class, DefaultDispatcher.class);

	public Initializer init(Options o) {
		options.merge(o);
		logger.info("Initializing the Portal application with options {}", options);
		
		scanHandler();
		for (Entry<String, Set<Class<?>>> entry : options.controllersPerApp().entrySet()) {
			processApp(entry.getKey(), entry.getValue());
		}
		
		for (Preparer preparer : preparers) {
			try {
				preparer.execute();
			} catch (Exception e) {
				logger.error("Failed to execute @Prepare method " + preparer.method, e);
			}
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	private void scanHandler() {
		String base = "";
		if (options.base() != null) {
			try {
				base = new File(options.base()).getCanonicalPath();
			} catch (IOException e) {
				logger.error("Cannot resolve the canonical path of the base " + options.base(), e);
			}
		}

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		AnnotationDetector detector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {

			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { Handler.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				try {
					options.controllers(classLoader.loadClass(className));
				} catch (ClassNotFoundException e) {
					logger.error("Controller class " + className + " not found", e);
				}
			}

		});
		if (options.locations() != null) {
			for (String location : options.locations()) {
				location = base + ((location.length() != 0 && location.charAt(0) != '/') ? "/" : "") + location;
				logger.debug("Scanning @Handler annotation in {}", location);

				try {
					detector.detect(new File(location));
				} catch (IOException e) {
					logger.error("Failed to scan in " + location, e);
				}
			}
		}
		if (options.packages() != null) {
			for (String packageName : options.packages()) {
				logger.debug("Scanning @Handler annotation in {}", packageName);

				try {
					detector.detect(packageName);
				} catch (IOException e) {
					logger.error("Failed to scan in " + packageName, e);
				}	
			}
		}
	}

	private void processApp(String name, Set<Class<?>> controllers) {
		logger.info("Processing an application {}", name);
		
		App app = App.add(new App(name));
		apps.put(name, app);
		
		for (Entry<Class<?>, Class<?>> entry : options.classes().entrySet()) {
			try {
				// TODO introduce ObjectFactory
				app.bean(entry.getKey(), entry.getValue().newInstance());
			} catch (Exception e) {
				logger.error("Failed to construct the implementation " + entry.getValue() + " of " + entry.getKey(), e);
			}
		}
		
		for (Class<?> controller : controllers) {
			processController(app, controller);
		}
		
		// TODO use AppAware
		if (app.bean(SocketManager.class) != null) {
			app.bean(SocketManager.class).setApp(app);
		}
	}

	private void processController(App app, Class<?> clazz) {
		logger.info("Processing a controller {}", clazz.getName());

		Object instance;
		try {
			instance = clazz.newInstance();
		} catch (Exception e) {
			logger.error("Failed to construct the controller " + clazz.getName(), e);
			return;
		}
		
		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Name.class)) {
				String name = field.getAnnotation(Name.class).value();
				Object value = null;
				logger.debug("@Name(\"{}\") on {}", name, field);

				field.setAccessible(true);
				if (field.getType() == App.class) {
					value = App.find(name);
				} else if (field.getType() == Room.class) {
					value = app.room(name);
				} else {
					logger.error("@Name can be present only on the fields whose type is App or Room");
				}

				try {
					field.set(instance, value);
				} catch (Exception e) {
					logger.error("Failed to set " + field + " to " + value, e);
				}
			}
		}
		
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(Prepare.class)) {
				logger.debug("@Prepare on {}", method);
				preparers.add(new Preparer(instance, method));
			}

			String on = null;
			if (method.isAnnotationPresent(On.class)) {
				on = method.getAnnotation(On.class).value();
				logger.debug("@On(\"{}\") on {}", on, method);
			} else {
				for (Annotation ann : method.getAnnotations()) {
					if (ann.annotationType().isAnnotationPresent(On.class)) {
						on = ann.annotationType().getAnnotation(On.class).value();
						logger.debug("@On(\"{}\") of {} on {}", on, ann, method);
						break;
					}
				}
			}

			if (on != null) {
				app.bean(Dispatcher.class).on(on, instance, method);
			}
		}
	}

	public Map<String, App> apps() {
		return Collections.unmodifiableMap(apps);
	}
	
	private static class Preparer {
		
		Object instance;
		Method method;

		public Preparer(Object instance, Method method) {
			this.instance = instance;
			this.method = method;
		}

		public void execute() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			method.invoke(instance);
		}

	}

}
