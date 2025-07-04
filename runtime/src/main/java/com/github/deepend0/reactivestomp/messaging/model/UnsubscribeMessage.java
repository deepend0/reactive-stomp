/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.deepend0.reactivestomp.messaging.model;

public class UnsubscribeMessage extends Message {

	private final String destination;

	public UnsubscribeMessage(String sessionId, String destination) {
		super(sessionId);
		this.destination = destination;
	}

	public String getDestination() {
		return destination;
	}
}
