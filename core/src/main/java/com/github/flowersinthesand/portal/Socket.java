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
import java.util.Map;

public class Socket {
	
	private App app;
	private Map<String, String> params;

	public Socket(String query, App app) {
		this.app = app;
		this.params = new LinkedHashMap<String, String>();
		for (String entity : query.split("&")) {
			String[] parts = entity.split("=", 2);
			this.params.put(parts[0], parts[1]);
		}
	}
	
	public boolean opened() {
		return app.socketManager().opened(this);
	}

	public String param(String key) {
		return params.get(key);
	}

	public Socket on(String event, Fn.Callback handler) {
		if (opened()) {
			app.eventDispatcher().on(event, this, handler);
		}
		return this;
	}

	public <A> Socket on(String event, Fn.Callback1<A> handler) {
		if (opened()) {
			app.eventDispatcher().on(event, this, handler);
		}
		return this;
	}

	public <A, B> Socket on(String event, Fn.Callback2<A, B> handler) {
		if (opened()) {
			app.eventDispatcher().on(event, this, handler);
		}
		return this;
	}

	public Socket send(String event) {
		if (opened()) {
			app.socketManager().send(this, event, null);
		}
		return this;
	}

	public Socket send(String event, Object data) {
		if (opened()) {
			app.socketManager().send(this, event, data);
		}
		return this;
	}

	public Socket send(String event, Object data, Fn.Callback callback) {
		if (opened()) {
			app.socketManager().send(this, event, data, callback);
		}
		return this;
	}

	public <A> Socket send(String event, Object data, Fn.Callback1<A> callback) {
		if (opened()) {
			app.socketManager().send(this, event, data, callback);
		}
		return this;
	}

	public void close() {
		if (opened()) {
			app.socketManager().close(this);
		}
	}

}
