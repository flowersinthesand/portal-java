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
package com.github.flowersinthesand.portal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Options {

	private String name;
	private String url;
	private Set<String> packages = new LinkedHashSet<String>();
	private Map<String, Object> beans = new LinkedHashMap<String, Object>();

	public String name() {
		return name != null ? name : url;
	}

	public Options name(String name) {
		this.name = name;
		return this;
	}

	public String url() {
		return url;
	}

	public Options url(String url) {
		this.url = url;
		return this;
	}

	public Set<String> packages() {
		return Collections.unmodifiableSet(packages);
	}

	public Options packageOf(String... packageNames) {
		for (String packageName : packageNames) {
			packages.add(packageName);
		}
		return this;
	}

	public Options packageOf(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			packageOf(clazz.getPackage().getName());
		}
		return this;
	}

	public Options packageOf(Object... objects) {
		for (Object object : objects) {
			packageOf(object.getClass());
		}
		return this;
	}

	public Map<String, Object> beans() {
		return Collections.unmodifiableMap(beans);
	}

	public Object bean(String name) {
		return beans.get(name);
	}

	public Options bean(String name, Object bean) {
		beans.put(name, bean);
		return this;
	}

	public String toString() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put("name", name());
		map.put("url", url());
		map.put("packages", packages());
		map.put("beans", beans());

		return map.toString();
	}

}
