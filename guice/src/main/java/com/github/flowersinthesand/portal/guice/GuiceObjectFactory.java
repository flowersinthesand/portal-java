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
package com.github.flowersinthesand.portal.guice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.support.NewObjectFactory;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;

public class GuiceObjectFactory extends NewObjectFactory {

	private final Logger logger = LoggerFactory.getLogger(GuiceObjectFactory.class);
	private Injector injector;

	public GuiceObjectFactory(Injector injector) {
		this.injector = injector;
	}

	@Override
	public <T> T create(String name, Class<T> clazz) {
		try {
			return injector.getInstance(clazz);
		} catch (ConfigurationException e) {
			logger.debug("{}", e.getMessage());
			return super.create(name, clazz);
		}
	}

}
