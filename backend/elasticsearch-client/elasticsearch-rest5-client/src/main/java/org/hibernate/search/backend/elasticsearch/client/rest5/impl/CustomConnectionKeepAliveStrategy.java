/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest5.impl;


import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

final class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

	private final TimeValue maxKeepAlive;
	private final long maxKeepAliveMilliseconds;

	CustomConnectionKeepAliveStrategy(long maxKeepAlive) {
		this.maxKeepAlive = TimeValue.of( maxKeepAlive, TimeUnit.MILLISECONDS );
		this.maxKeepAliveMilliseconds = maxKeepAlive;
	}

	@Override
	public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
		// get a keep alive from a request header if one is present
		TimeValue keepAliveDuration = DefaultConnectionKeepAliveStrategy.INSTANCE.getKeepAliveDuration( response, context );
		long keepAliveDurationMilliseconds = keepAliveDuration.toMilliseconds();
		// if the keep alive timeout from a request is less than configured one - let's honor it:
		if ( keepAliveDurationMilliseconds > 0 && keepAliveDurationMilliseconds < maxKeepAliveMilliseconds ) {
			return keepAliveDuration;
		}
		return maxKeepAlive;
	}
}
