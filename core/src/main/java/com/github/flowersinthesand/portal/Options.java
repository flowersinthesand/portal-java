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

	private Set<Class<?>> handlers = new LinkedHashSet<Class<?>>();
	private Set<String> packages = new LinkedHashSet<String>();
	private String base;
	private Set<String> locations = new LinkedHashSet<String>();
	private Map<String, Class<?>> classes = new LinkedHashMap<String, Class<?>>();
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();

	public Set<Class<?>> handlers() {
		return handlers;
	}

	public Options handlers(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			handlers.add(clazz);
		}
		return this;
	}

	public Set<String> packages() {
		return packages;
	}

	public Options packages(String... packageNames) {
		for (String packageName : packageNames) {
			packages.add(packageName);
		}
		return this;
	}

	public String base() {
		return base;
	}

	public Options base(String base) {
		this.base = base;
		return this;
	}

	public Set<String> locations() {
		return locations;
	}

	public Options locations(String... paths) {
		for (String path : paths) {
			locations.add(path);
		}
		return this;
	}

	public Map<String, Class<?>> classes() {
		return classes;
	}

	public Options classes(String name, Class<?> clazz) {
		classes.put(name, clazz);
		return this;
	}

	public Options classes(String name1, Class<?> clazz1, String name2, Class<?> clazz2) {
		return classes(name1, clazz1).classes(name2, clazz2);
	}

	public Map<String, Object> beans() {
		return beans;
	}

	public Object bean(String key) {
		return beans.get(key);
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

	public Options beans(Object... objects) {
		for (Object object : objects) {
			beans(object.getClass().getName(), object);
		}
		return this;
	}

	public Options beans(String name, Object bean) {
		beans.put(name, bean);
		return this;
	}

	public Options merge(Options options) {
		if (options != null) {
			if (options.handlers() != null) {
				handlers.addAll(options.handlers());
			}
			if (options.packages() != null) {
				packages.addAll(options.packages());
			}
			if (options.base() != null) {
				base = options.base();
			}
			if (options.locations() != null) {
				locations.addAll(options.locations());
			}
			if (options.classes() != null) {
				classes.putAll(options.classes());
			}
			if (options.classes() != null) {
				classes.putAll(options.classes());
			}
			if (options.beans() != null) {
				beans.putAll(options.beans());
			}
		}

		return this;
	}

	public String toString() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put("handlers", handlers);
		map.put("packages", packages);
		map.put("base", base);
		map.put("locations", locations);
		map.put("classes", classes);
		map.put("beans", beans);

		return map.toString();
	}

}
