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

import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.ServerHttpExchange;
import io.github.flowersinthesand.wes.ServerWebSocket;

/**
 * Interface used to interact with the socket.
 * <p>
 * A {@code Server} instance provides {@link Socket} processing HTTP request and
 * WebSocket under the specific URI pattern and manages their life cycles. The
 * {@code Server} API is used to accept socket and to find socket by id and tag.
 * If you are using dependency injection support, make a {@code Server} as
 * component and inject it wherever you need to handle socket.
 * <p>
 * The {@code Server} is a wes application so can be installed on any platform
 * like Servlet wes supports. For that reason, {@code Server} doesn't concern
 * I/O details and I/O details should be configured in the platform following
 * its policy.
 * <p>
 * Server may be accessed by multiple threads.
 * 
 * @author Donghwan Kim
 * @see <a
 *      href="https://github.com/flowersinthesand/portal-java-examples/tree/master/server/platform/"
 *      target="_parent">Examples to install portal</a>
 */
public interface Server {

	/**
	 * Executes the given action retrieving all of the socket in this server.
	 */
	Server all(Action<Socket> action);

	/**
	 * Executes the given action retrieving the socket of the given id. The
	 * given action will be executed only once if socket is found and won't be
	 * executed if not found.
	 */
	Server byId(String id, Action<Socket> action);

	/**
	 * Executes the given action retrieving the socket tagged with the given
	 * name. The given action will be executed multiple times if sockets are
	 * found and won't be executed if not found.
	 */
	Server byTag(String name, Action<Socket> action);

	/**
	 * Executes the given action retrieving the socket tagged with all of the
	 * given names. The given action will be executed multiple times if sockets
	 * are found and won't be executed if not found.
	 */
	Server byTag(String[] names, Action<Socket> action);

	/**
	 * Registers an action to be called when the socket has been opened. It's
	 * allowed to add several actions before and after installation, so you
	 * don't need to centralize all your code to one class.
	 */
	Server socketAction(Action<Socket> action);

	/**
	 * ServerHttpExchange action to install in wes
	 */
	Action<ServerHttpExchange> httpAction();

	/**
	 * ServerWebSocket action to install in wes
	 */
	Action<ServerWebSocket> websocketAction();

}