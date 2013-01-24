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
package com.github.flowersinthesand.portal.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.github.flowersinthesand.portal.support.NewObjectFactory;

public class SpringObjectFactory extends NewObjectFactory {

	private final Logger logger = LoggerFactory.getLogger(SpringObjectFactory.class);
	private BeanFactory beanFactory;

	public SpringObjectFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public <T> T create(String name, Class<T> clazz) {
		try {
			return beanFactory.getBean(name, clazz);
		} catch (NoSuchBeanDefinitionException e) {
			logger.debug("{}", e.getMessage());
			try {
				return beanFactory.getBean(clazz);
			} catch (NoSuchBeanDefinitionException ex) {
				logger.debug("{}", ex.getMessage());
				return super.create(name, clazz);
			}
		}
	}

}
