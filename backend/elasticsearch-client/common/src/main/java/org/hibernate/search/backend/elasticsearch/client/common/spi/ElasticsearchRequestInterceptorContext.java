/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ElasticsearchRequestInterceptorContext {

	boolean hasContent();

	InputStream content() throws IOException;

	String scheme();

	String host();

	Integer port();

	String method();

	String path();

	Map<String, String> queryParameters();

	void overrideHeaders(Map<String, List<String>> headers);

	/**
	 * @return A String representation of the wrapped request. Primarily used for logging the request.
	 */
	String toString();
}
