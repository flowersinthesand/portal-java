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
package com.github.flowersinthesand.portal.spi;

import java.lang.reflect.Method;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;

public interface Dispatcher {

	void on(String event, Object handler, Method method);

	void on(String event, Socket socket, Fn.Callback handler);

	void on(String event, Socket socket, Fn.Callback1<?> handler);

	void on(String event, Socket socket, Fn.Callback2<?, ?> handler);

	void fire(String on, Socket socket);

	void fire(String on, Socket socket, Object data);

	void fire(String on, Socket socket, Object data, Fn.Callback1<Object> reply);

}