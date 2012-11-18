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
package org.flowersinthesand.portal.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.flowersinthesand.portal.Handler;
import org.flowersinthesand.portal.On;
import org.flowersinthesand.portal.Prepare;
import org.flowersinthesand.portal.dispatcher.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.infomas.annotation.AnnotationDetector;

public class Initializer {

	private final Logger logger = LoggerFactory.getLogger(InitializerContextListener.class);
	private Dispatcher dispatcher;

	Initializer(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	void init(String packageName) throws IOException {
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
						process(className);
					} catch (Exception e) {
						logger.warn("", e);
					}
				}
			}

			void process(String className) throws InstantiationException, IllegalAccessException,
					ClassNotFoundException, IllegalArgumentException, InvocationTargetException {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
				String handler = clazz.getAnnotation(Handler.class).value();
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
						dispatcher.on(handler, on, instance, method);
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
	}

}
