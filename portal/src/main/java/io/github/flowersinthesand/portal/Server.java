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
 * Server for Portal.
 * <p>
 * Server is a server-side wes application which provides and manages
 * {@link Socket} processing HTTP request and WebSocket so that it can run on
 * any framework wes supports. See <a
 * href="http://flowersinthesand.github.io/wes" target="_parent">wes</a>
 * documentation for how to install and what frameworks are supported.
 * <p>
 * If you are using dependency injection framework like Spring, you can create
 * Server as component of singleton scope and inject it where you need to
 * communicate with client in real time.
 * <p>
 * Server may be accessed by multiple threads.
 * 
 * @author Donghwan Kim
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
	 * Registers an action to be called when the socket has been opened.
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