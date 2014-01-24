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

/**
 * {@code AbstractSocket} consists of a set of common functionalities of
 * {@link Sentence} and {@link Socket}.
 * 
 * @author Donghwan Kim
 */
public interface AbstractSocket<T> {

	/**
	 * Sends a given event without data.
	 */
	T send(String event);

	/**
	 * Sends a given event with data.
	 */
	T send(String event, Object data);

	/**
	 * Closes the socket.
	 */
	T close();

	/**
	 * Attaches given tags to the socket.
	 */
	T tag(String... names);

	/**
	 * Detaches given tags from the socket.
	 */
	T untag(String... names);

}
