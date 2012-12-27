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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.atmosphere.AtmosphereSocketManager;

import eu.infomas.annotation.AnnotationDetector;

public class Initializer {

	private final Logger logger = LoggerFactory.getLogger(Initializer.class);
	private Map<String, App> apps = new LinkedHashMap<String, App>();
	private List<Preparer> preparers = new ArrayList<Preparer>();

	public Initializer init(String packageName) throws IOException {
		logger.info("Initializing the Portal application");
		
		AnnotationDetector detector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { Handler.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				try {
					Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
					String name = clazz.getAnnotation(Handler.class).value();
					
					if (!apps.containsKey(name)) {
						apps.put(name, createApp(name));
					}
					
					process(apps.get(name), clazz);
				} catch (ClassNotFoundException e) {
					logger.error("", e);
				}
			}
		});
		
		if (packageName != null) {
			logger.info("Scanning the class path under {}", packageName);
			detector.detect(packageName);
		} else {
			logger.info("Scanning the class path");
			detector.detect();
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

	private App createApp(String name) {
		App app = new App();
		App.add(name, app);
		
		AtmosphereSocketManager socketManager = new AtmosphereSocketManager();
		socketManager.setApp(app);
		
		return app
			.set(App.NAME, name)
			.set(App.EVENT_DISPATCHER, new DefaultEventDispatcher())
			.set(App.SOCKET_MANAGER, socketManager);
	}

	private void process(App app, Class<?> clazz) {
		logger.info("Processing @Handler(\"{}\") on {}", app.name(), clazz.getName());

		Object instance;
		try {
			instance = clazz.newInstance();
		} catch (Exception e) {
			logger.error("Failed to construct " + clazz.getName(), e);
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
				app.eventDispatcher().on(on, instance, method);
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
