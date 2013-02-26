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
import play.Application;
import play.GlobalSettings;
import play.api.mvc.Handler;
import play.mvc.Http.RequestHeader;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.play.Handlers;
import com.github.flowersinthesand.portal.play.PlayModule;

public class Global extends GlobalSettings {

	private App app;

	@Override
	public void onStart(Application application) {
		app = new App(new Options().url("/chat").packageOf("com.github.flowersinthesand.portal.samples.chat"), new PlayModule());
	}

	@Override
	public void onStop(Application application) {
		app.close();
	}

	@Override
	public Handler onRouteRequest(RequestHeader request) {
		return Handlers.get(request);
	}

}
