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
		AnnotationDetector detector = new AnnotationDetector(new AnnotationDetector.TypeReporter() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { Handler.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				if (Handler.class.equals(annotation)) {
					try {
						Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
						String name = clazz.getAnnotation(Handler.class).value();

						if (!apps.containsKey(name)) {
							apps.put(name, create(name));
						}

						process(apps.get(name), clazz);
					} catch (Exception e) {
						logger.warn("", e);
					}
				}
			}
		});

		// Detects @Handler
		if (packageName != null) {
			detector.detect(packageName);
		} else {
			detector.detect();
		}

		// Executes @Prepare methods
		for (Preparer preparer : preparers) {
			try {
				preparer.execute();
			} catch (Exception e) {
				logger.warn("", e);
			}
		}

		return this;
	}

	private App create(String name) {
		App app = new App();
		App.add(name, app);
		
		AtmosphereSocketManager socketManager = new AtmosphereSocketManager();
		socketManager.setApp(app);
		
		return app
			.set(App.NAME, name)
			.set(App.EVENT_DISPATCHER, new DefaultEventDispatcher())
			.set(App.SOCKET_MANAGER, socketManager);
	}

	private void process(App app, Class<?> clazz) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object instance = clazz.newInstance();
		
		// Finds @Name
		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Name.class)) {
				String value = field.getAnnotation(Name.class).value();
				field.setAccessible(true);
				if (field.getType() == App.class) {
					field.set(instance, App.find(value));
				} else if (field.getType() == Room.class) {
					field.set(instance, app.room(value));
				} else {
					// TODO throw new exception
				}
			}
		}
		
		// Finds @Prepare and @On
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(Prepare.class)) {
				preparers.add(new Preparer(instance, method));
			}

			String on = null;
			if (method.isAnnotationPresent(On.class)) {
				on = method.getAnnotation(On.class).value();
			} else {
				// Such as @On.open, @On.close
				for (Annotation ann : method.getAnnotations()) {
					if (ann.annotationType().isAnnotationPresent(On.class)) {
						on = ann.annotationType().getAnnotation(On.class).value();
						break;
					}
				}
			}

			// Register event
			if (on != null) {
				app.getEventDispatcher().on(on, instance, method);
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
