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

import com.github.flowersinthesand.portal.spi.ObjectFactory;

public class Options {

	private Set<Class<?>> controllers;
	private Set<String> packages;
	private String base;
	private Set<String> locations;
	private ObjectFactory objectFactory;
	private Map<Class<?>, Class<?>> classes = new LinkedHashMap<Class<?>, Class<?>>();

	public Set<Class<?>> controllers() {
		return controllers;
	}

	public Options controllers(Class<?>... classes) {
		if (controllers == null) {
			controllers = new LinkedHashSet<Class<?>>();
		}
		for (Class<?> clazz : classes) {
			controllers.add(clazz);
		}
		return this;
	}

	Map<String, Set<Class<?>>> controllersPerApp() {
		Map<String, Set<Class<?>>> answer = new LinkedHashMap<String, Set<Class<?>>>();
		if (controllers != null) {
			for (Class<?> controller : controllers) {
				String name = controller.getAnnotation(Handler.class).value();

				if (!answer.containsKey(name)) {
					answer.put(name, new LinkedHashSet<Class<?>>());
				}

				answer.get(name).add(controller);
			}
		}
		
		return answer;
	}

	public Set<String> packages() {
		return packages;
	}

	public Options packages(String... packageNames) {
		if (packages == null) {
			packages = new LinkedHashSet<String>();
		}
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
		if (locations == null) {
			locations = new LinkedHashSet<String>();
		}
		for (String path : paths) {
			locations.add(path);
		}
		return this;
	}

	public ObjectFactory objectFactory() {
		return objectFactory;
	}

	public Options objectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
		return this;
	}

	public Map<Class<?>, Class<?>> classes() {
		return classes;
	}

	public <A> Options classes(Class<A> spec, Class<? extends A> impl) {
		classes.put(spec, impl);
		return this;
	}

	public <A, B> Options classes(Class<A> spec1, Class<? extends A> impl1, Class<B> spec2, Class<? extends B> impl2) {
		return classes(spec1, impl1).classes(spec2, impl2);
	}

	public Options merge(Options that) {
		if (that.controllers != null) {
			this.controllers = that.controllers;
		}
		if (that.packages != null) {
			this.packages = that.packages;
		}
		if (that.base != null) {
			this.base = that.base;
		}
		if (that.locations != null) {
			this.locations = that.locations;
		}
		if (that.objectFactory != null) {
			this.objectFactory = that.objectFactory;
		}
		this.classes.putAll(that.classes);
		return this;
	}

	@Override
	public String toString() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("controllers", controllers);
		map.put("packages", packages);
		map.put("base", base);
		map.put("locations", locations);
		map.put("objectFactory", objectFactory);
		map.put("classes", classes);
		return map.toString();
	}

}
