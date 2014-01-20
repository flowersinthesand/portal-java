/*
 * Copyright 2012-2014 Donghwan Kim
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
package io.github.flowersinthesand.portal;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.Actions;
import io.github.flowersinthesand.wes.ConcurrentActions;

/**
 * {@link Server} implementation for clustering.
 * <p>
 * This implementation follows the publish and subscribe model from Java Message
 * Service (JMS) to support clustering. Here, the message represents invocation
 * of socket action and is created when one of selector actions is called. The
 * publisher should publish the message passed from
 * {@link ClusteredServer#publishAction(Action)} to all nodes in cluster and the
 * subscriber should propagate a message sent from one of node in cluster to
 * {@link ClusteredServer#messageAction()}.
 * <p>
 * An invocation of the following socket finder actions will propagate to all
 * the server in cluster:
 * <ul>
 * <li>{@link ClusteredServer#all(Action)}</li>
 * <li>{@link ClusteredServer#byId(Action)}</li>
 * <li>{@link ClusteredServer#byTag(Action)}</li>
 * </ul>
 * That means {@code server.all(action)} executes a given action with not only
 * all the sockets in this server but also all the sockets in all the other
 * servers in the cluster.
 * <p>
 * Accordingly, most of Message Oriented Middlware requires message to be
 * serialized and you may have to have pass {@link Action} implementing
 * {@link Serializable} on method call. See the provided link, serialization of
 * inner classes including local and anonymous classes, is discouraged and
 * doesn't work in some cases. Therefore, always use {@link Sentence} instead of action
 * if possible.
 * 
 * @author Donghwan Kim
 * @see Sentence
 * @see <a
 *      href="http://docs.oracle.com/javase/7/docs/platform/serialization/spec/serial-arch.html#4539">Note
 *      of the Serializable Interface</a>
 */
public class ClusteredServer extends DefaultServer {

	private Actions<Map<String, Object>> publishActions = new ConcurrentActions<>();
	private Action<Map<String, Object>> messageAction = new Action<Map<String, Object>>() {
		@SuppressWarnings("unchecked")
		@Override
		public void on(Map<String, Object> map) {
			String methodName = (String) map.get("method");
			Object[] args = (Object[]) map.get("args");
			switch (methodName) {
			case "all":
				ClusteredServer.super.all((Action<Socket>) args[0]);
				break;
			case "byId":
				ClusteredServer.super.byId((String) args[0], (Action<Socket>) args[1]);
				break;
			case "byTag":
				ClusteredServer.super.byTag((String[]) args[0], (Action<Socket>) args[1]);
				break;
			default:
				throw new IllegalArgumentException("Illegal method name in processing message: " + methodName);
			}
		}
	};

	@Override
	public Server all(Action<Socket> action) {
		publishMessage("all", action);
		return this;
	}

	@Override
	public Server byId(String id, Action<Socket> action) {
		publishMessage("byId", id, action);
		return this;
	}

	@Override
	public Server byTag(String[] names, Action<Socket> action) {
		publishMessage("byTag", names, action);
		return this;
	}

	private void publishMessage(String method, Object... args) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("method", method);
		map.put("args", args);
		publishActions.fire(Collections.unmodifiableMap(map));
	}

	/**
	 * Attaches an action to be called with a map containing method name and
	 * arguments of socket action when it's called.
	 */
	public Server publishAction(Action<Map<String, Object>> action) {
		publishActions.add(action);
		return this;
	}

	/**
	 * This action receives a map fired from one of node in cluster and invokes
	 * socket action in this server.
	 */
	public Action<Map<String, Object>> messageAction() {
		return messageAction;
	}

}
