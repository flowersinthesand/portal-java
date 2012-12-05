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
package org.flowersinthesand.portal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.infomas.annotation.AnnotationDetector;

public class Initializer {

	private final Logger logger = LoggerFactory.getLogger(Initializer.class);
	private Map<String, Map<String, Object>> apps = new LinkedHashMap<String, Map<String,Object>>();

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
						String app = clazz.getAnnotation(Handler.class).value();

						if (!apps.containsKey(app)) {
							apps.put(app, new LinkedHashMap<String, Object>());
							apps.get(app).put("events", new Events());
							apps.get(app).put("rooms", new Rooms());
							Rooms.add(app, (Rooms) apps.get(app).get("rooms"));
						}

						process(app, clazz);
					} catch (Exception e) {
						logger.warn("", e);
					}
				}
			}

			void process(String app, Class<?> clazz) throws InstantiationException,
					IllegalAccessException, IllegalArgumentException, InvocationTargetException {
				Object instance = clazz.newInstance();

				for (Method method : clazz.getMethods()) {
					// Executes @Prepare
					if (method.isAnnotationPresent(Prepare.class)) {
						method.invoke(instance);
					}

					// Finds @On
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
						((Events) apps.get(app).get("events")).on(app, on, instance, method);
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

		for (Entry<String, Map<String, Object>> entry : apps.entrySet()) {
			apps.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
		}

		return this;
	}

	public Map<String, Map<String, Object>> apps() {
		return Collections.unmodifiableMap(apps);
	}

}
