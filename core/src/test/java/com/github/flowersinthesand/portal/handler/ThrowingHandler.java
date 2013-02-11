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
package com.github.flowersinthesand.portal.handler;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Throw;

@Bean
public class ThrowingHandler {

	@On
	@Reply
	@Throw
	public void success() throws TestException {}

	@On
	@Reply
	@Throw
	public void fail1() throws TestException {
		throw new TestException("Hello");
	}

	@On
	@Reply
	@Throw(TestException.class)
	public void fail2() {
		throw new TestException("Hello");
	}

	@On
	@Reply
	public void fail3() {
		throw new TestException("Hello");
	}

	@SuppressWarnings("serial")
	public static class TestException extends RuntimeException {

		public TestException() {
			super();
		}

		public TestException(String message, Throwable cause) {
			super(message, cause);
		}

		public TestException(String message) {
			super(message);
		}

		public TestException(Throwable cause) {
			super(cause);
		}

	}

}
