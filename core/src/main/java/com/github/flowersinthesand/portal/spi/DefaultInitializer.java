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
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.Prepare;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Wire;

public class DefaultInitializer extends InitializerAdapter {

	private final Logger logger = LoggerFactory.getLogger(DefaultInitializer.class);
	private App app;
	
	@Override
	public void init(App app, Options options) {
		this.app = app;
		options.packages("com.github.flowersinthesand.portal");
	}

	@Override
	public Object instantiate(String name, Class<?> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			logger.error("Failed to create the bean " + clazz.getName(), e);
		}

		return null;
	}
	
	@Override
	public void postInstantiation(String name, Object bean) {
		for (Field field : bean.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Wire.class)) {
				String beanName = field.getAnnotation(Wire.class).value();
				Class<?> beanType = field.getType();
				logger.debug("@Wire(\"{}\") on {}", beanName, field);

				Object value = null;
				if (beanType.isAssignableFrom(App.class)) {
					value = app;
				} else if (beanType.isAssignableFrom(Room.class)) {
					if (beanName.length() == 0) {
						throw new IllegalArgumentException("Room has no name in @Wire(\"\") " + field);
					}
					value = app.room(beanName);
				} else {
					value = beanName.length() > 0 ? app.bean(beanName) : app.bean(beanType);
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
		}
		
		for (Method method : bean.getClass().getMethods()) {
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
				Dispatcher dispatcher = (Dispatcher) app.bean(Dispatcher.class.getName());
				dispatcher.on(on, bean, method);
			}
			
			if (method.isAnnotationPresent(Prepare.class)) {
				logger.debug("@Prepare on {}", method);
				try {
					method.invoke(bean);
				} catch (Exception e) {
					logger.error("Failed to execute @Prepare method " + method, e);
					throw new RuntimeException(e);
				}
			}
		}
	}
	
}