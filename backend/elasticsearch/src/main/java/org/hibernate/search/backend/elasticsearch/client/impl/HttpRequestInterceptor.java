/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.net.http.HttpRequest;

interface HttpRequestInterceptor {
	void process(HttpRequest.Builder request, HttpRequest.BodyPublisher bodyPublisher, HttpRequestInterceptorContext context)
			throws IOException;
}
