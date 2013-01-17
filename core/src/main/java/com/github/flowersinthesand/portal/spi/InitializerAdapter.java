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

import java.util.Map;
import java.util.Set;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Options;

public abstract class InitializerAdapter implements Initializer {

	@Override
	public Options init(App app, Map<String, Object> props) {
		return null;
	}
	
	@Override
	public Object instantiateBean(Class<?> spec, Class<?> impl) {
		return null;
	}

	@Override
	public void postBeansInstantiation(Map<Class<?>, Object> beans) {}

	@Override
	public Object instantiateHandler(Class<?> clazz) {
		return null;
	}

	@Override
	public void postHandlersInstantiation(Set<Object> handlers) {}

}