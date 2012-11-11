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

import java.util.Set;

public abstract class Room {

	public static Room find(String name) {
		// TODO
		return null;
	}

	public static Room open(String name) {
		// TODO
		return null;
	}

	public static Room findOrOpen(String name) {
		// TODO
		return null;
	}

	public abstract void close();

	public abstract String name();

	public abstract Room add(Socket socket);

	public abstract Room remove(Socket socket);

	public abstract Set<Socket> sockets();

	public abstract Set<Socket> sockets(Fn.Feedback1<Boolean, Socket> filter);

	public abstract int size();

	public abstract Room clear();

	public abstract Object get(String key);

	public abstract Room set(String key, Object value);

}
