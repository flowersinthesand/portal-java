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
package com.github.flowersinthesand.portal.play;

import play.api.mvc.Codec;
import play.core.j.JavaResults;
import play.libs.F;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Results.Chunks;
import play.mvc.WebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketFactory")
public class PlaySocketFactory extends AbstractSocketFactory {

	WebSocket<String> openWs(Request req) {
		WsSocket socket = new WsSocket(req);
		sockets.put(socket.id(), socket);

		return socket.webSocket;
	}

	Chunks<String> openHttp(Request req, Response res) {
		String when = req.queryString().get("when")[0];
		String id = req.queryString().get("id")[0];
		String transport = req.queryString().get("transport")[0];

		HttpSocket socket = null;
		if (transport.equals("sse") || transport.startsWith("stream")) {
			socket = new StreamSocket(req, res);
			sockets.put(id, socket);
		} else if (transport.startsWith("longpoll")) {
			if (when.equals("open")) {
				socket = new LongPollSocket(req, res);
				sockets.put(id, socket);
			} else if (when.equals("poll")) {
				socket = (LongPollSocket) sockets.get(id);
				((LongPollSocket) socket).refresh(req, res, false);
			}
		}

		return socket.chunks;
	}

	class WsSocket extends AbstractSocket {

		private WebSocket<String> webSocket;
		private WebSocket.Out<String> out;

		public WsSocket(Request req) {
			this.params = params(req.queryString());
			this.webSocket = new WebSocket<String>() {
				@Override
				public void onReady(WebSocket.In<String> in, Out<String> oout) {
					out = oout;
					in.onClose(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});
					in.onMessage(new F.Callback<String>() {
						@Override
						public void invoke(String message) throws Throwable {
							fire(message);
						}
					});
					onOpen();
				}
			};
		}

		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}

	}

	abstract class HttpSocket extends AbstractSocket {

		protected Chunks<String> chunks;
		protected Chunks.Out<String> out;

	}

	class StreamSocket extends HttpSocket {

		public StreamSocket(Request req, Response res) {
			this.params = params(req.queryString());
			this.isAndroid = isAndroid(req.getHeader("user-agent"));
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});

					out.write(padding2K);
					if (isAndroid) {
						out.write(padding2K);
					}
					out.write("\n");
					onOpen();
				}
			};
			res.setContentType(streamContentType() + "; charset=utf-8");
		}

		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
			// onDisconnected is not fired by close method 
			onClose();
		}

	}

	class LongPollSocket extends HttpSocket {
		
		public LongPollSocket(Request req, Response res) {
			this.params = params(req.queryString());
			refresh(req, res, true);
		}

		private void refresh(final Request req, Response res, final boolean open) {
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							if (!open && out != null) {
								onClose();
							}
						}
					}); 
					
					if (open) {
						out.close();
						onOpen();
					} else {
						String[] value = req.queryString().get("lastEventIds");
						retrieveCache(value != null ? value[0] : null);
					}
				}
			};
			res.setContentType(longpollContentType() + "; charset=utf-8");
		}

		@Override
		protected void transmit(String it) {
			if (out != null) {
				Chunks.Out<String> oout = out;
				out = null;
				oout.write(it);
				oout.close();
			}
		}

		@Override
		protected void disconnect() {
			if (out != null) {
				out.close();
				// onDisconnected is not fired by close method 
				onClose();
			}
		}

	}

}
