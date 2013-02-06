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

import java.util.ArrayList;
import java.util.List;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Order;

@Bean
public class OrderHandler {

	public List<Integer> args = new ArrayList<Integer>();

	@On("x")
	@Order(-1)
	public void x1() {
		args.add(-1);
	}

	@On("x")
	public void x2() {
		args.add(0);
	}

	@On("x")
	@Order(1)
	public void x3() {
		args.add(1);
	}

	@On("y")
	@Order(1)
	public void y3() {
		args.add(1);
	}

	@On("y")
	@Order(0)
	public void y2() {
		args.add(0);
	}

	@On("y")
	@Order(-1)
	public void y1() {
		args.add(-1);
	}

}
