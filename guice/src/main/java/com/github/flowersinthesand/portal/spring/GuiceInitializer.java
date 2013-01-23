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
package com.github.flowersinthesand.portal.spring;

import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.Initializer;
import com.github.flowersinthesand.portal.spi.ObjectFactory;
import com.google.inject.Injector;

public class GuiceInitializer implements Initializer {

	@Override
	public void init(Options options) {
		options.beans(ObjectFactory.class.getName(), new GuiceObjectFactory(options.bean(Injector.class)));
	}

}
