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
package com.github.flowersinthesand.portal.handler;

import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;

public class EventsHandler {

	public Object[] args;

	@On("load")
	public void onLoad() {}
	
	@On("socket")
	public void onSocket(Socket socket) {
		args = new Object[] { socket };
	}
	
	@On("data")
	public void onData(@Data DataBean data) {
		args = new Object[] { data };
	}

	@On("repli")
	public void onRepli(@Reply Fn.Callback reply) {
		args = new Object[] { reply };
		reply.call();
	}

	@On("repli-data")
	public void onRepliData(@Reply Fn.Callback1<Object> reply, @Data DataBean data) {
		args = new Object[] { reply, data };
		reply.call(data);
	}

	@On("socket-data-repli")
	public void onSocketDataRepli(Socket socket, @Data DataBean data, @Reply Fn.Callback1<Object> reply) {
		args = new Object[] { socket, data, reply };
		reply.call(data);
	}
	
}
