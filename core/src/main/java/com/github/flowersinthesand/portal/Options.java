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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Options {

	private Set<String> packages = new LinkedHashSet<String>();
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();

	public Set<String> packages() {
		return packages;
	}

	public Options packages(String... packageNames) {
		for (String packageName : packageNames) {
			packages.add(packageName);
		}
		return this;
	}

	public Map<String, Object> beans() {
		return beans;
	}

	public Object bean(String name) {
		return beans.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(Class<? super T> clazz) {
		for (Object object : beans.values()) {
			if (clazz == object.getClass()) {
				return (T) object;
			}
		}
		for (Object object : beans.values()) {
			if (clazz.isAssignableFrom(object.getClass())) {
				return (T) object;
			}
		}
		return null;
	}

	public Options beans(String name, Object bean) {
		beans.put(name, bean);
		return this;
	}

	public Options beans(Object... beans) {
		for (Object bean : beans) {
			beans(bean.getClass().getName(), bean);
		}
		return this;
	}

	public Options merge(Options options) {
		if (options != null) {
			if (options.packages() != null) {
				packages.addAll(options.packages());
			}
			if (options.beans() != null) {
				beans.putAll(options.beans());
			}
		}

		return this;
	}

	public String toString() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put("packages", packages);
		map.put("beans", beans);

		return map.toString();
	}

}
