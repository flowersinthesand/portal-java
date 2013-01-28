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

public class DataBean {
	
	private int number;
	private String string;
	
	public DataBean() {}

	public DataBean(int number, String string) {
		this.number = number;
		this.string = string;
	}

	public int getNumber() {
		return number;
	}

	public String getString() {
		return string;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DataBean)) {
			return false;
		}

		DataBean bean = (DataBean) obj;
		return bean.number != this.number ? 
			false : 
			bean.string == null ? 
				this.string == null : 
				bean.string.equals(this.string);
	}

}
