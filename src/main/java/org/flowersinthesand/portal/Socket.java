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
package org.flowersinthesand.portal;

import java.util.Map;

import org.flowersinthesand.portal.Fn;

public class Socket {

	private String id;
	private App app;
	private Map<String, String[]> params;

	public Socket(String id, App app, Map<String, String[]> params) {
		this.id = id;
		this.app = app;
		this.params = params;
	}

	public String id() {
		return id;
	}

	public boolean opened() {
		return app.sockets().opened(this);
	}

	public String param(String key) {
		String[] param = params.get(key);
		return param == null ? null : param[0];
	}

	public Socket on(String event, Fn.Callback handler) {
		app.events().on(event, this, handler);
		return this;
	}

	public <A> Socket on(String event, Fn.Callback1<A> handler) {
		app.events().on(event, this, handler);
		return this;
	}

	public <A, B> Socket on(String event, Fn.Callback2<A, B> handler) {
		app.events().on(event, this, handler);
		return this;
	}

	public Socket send(String event) {
		return send(event, null);
	}

	public Socket send(String event, Object data) {
		app.sockets().send(this, event, data);
		return this;
	}

	public Socket send(String event, Object data, Fn.Callback callback) {
		app.sockets().send(this, event, data, callback);
		return this;
	}

	public <A> Socket send(String event, Object data, Fn.Callback1<A> callback) {
		app.sockets().send(this, event, data, callback);
		return this;
	}

	public void close() {
		app.sockets().close(this);
	}

}
