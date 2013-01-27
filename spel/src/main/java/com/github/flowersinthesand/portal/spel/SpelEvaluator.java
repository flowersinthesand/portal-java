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
package com.github.flowersinthesand.portal.spel;

import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;

@Bean("com.github.flowersinthesand.portal.spi.Dispatcher$Evaluator")
public class SpelEvaluator implements Dispatcher.Evaluator {

	@Wire
	private StandardEvaluationContext context;

	@Override
	public Object evaluate(Map<String, Object> root, String expression) {
		return new SpelExpressionParser().parseExpression(expression).getValue(this.context, root);
	}

}
