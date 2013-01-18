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
package com.github.flowersinthesand.portal.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Name;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.Prepare;
import com.github.flowersinthesand.portal.Room;

public class DefaultInitializer extends InitializerAdapter {

	private final Logger logger = LoggerFactory.getLogger(DefaultInitializer.class);
	private App app;
	private Dispatcher dispatcher;
	
	@Override
	public void init(App app, Options options) {
		this.app = app;
		options.classes(Dispatcher.class.getName(), DefaultDispatcher.class);
	}

	@Override
	public Object instantiateBean(String name, Class<?> clazz) {
		return instantiate(clazz);
	}
	
	@Override
	public void postBeanInstantiation(String name, Object bean) {
		if (Dispatcher.class.isAssignableFrom(bean.getClass())) {
			dispatcher = (Dispatcher) bean;
		}
	}

	@Override
	public Object instantiateHandler(Class<?> clazz) {
		return instantiate(clazz);
	}
	
	public Object instantiate(Class<?> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			logger.error("Failed to create the bean " + clazz.getName(), e);
		}

		return null;
	}

	@Override
	public void postHandlerInstantiation(Object handler) {
		logger.info("Installing a handler {}", handler);
		
		for (Field field : handler.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Name.class)) {
				String name = field.getAnnotation(Name.class).value();
				Object value = null;
				logger.debug("@Name(\"{}\") on {}", name, field);

				field.setAccessible(true);
				if (field.getType() == Room.class) {
					value = app.room(name);
				} else {
					logger.error("@Name can be present only on the fields whose type is Room");
				}

				try {
					field.set(handler, value);
				} catch (Exception e) {
					logger.error("Failed to set " + field + " to " + value, e);
				}
			}
		}
		
		for (Method method : handler.getClass().getMethods()) {
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
				dispatcher.on(on, handler, method);
			}
		}
		
		for (Method method : handler.getClass().getMethods()) {
			if (method.isAnnotationPresent(Prepare.class)) {
				logger.debug("@Prepare on {}", method);
				try {
					method.invoke(handler);
				} catch (Exception e) {
					logger.error("Failed to execute @Prepare method " + method, e);
				}
			}
		}
	}
	
}