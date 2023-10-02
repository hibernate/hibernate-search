/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link ElasticsearchRequest}s in log messages.
 *
 */
public final class ElasticsearchRequestFormatter {

	private final ElasticsearchRequest request;

	public ElasticsearchRequestFormatter(ElasticsearchRequest request) {
		this.request = request;
	}

	@Override
	public String toString() {
		return formatRequest( request );
	}

	private static String formatRequest(ElasticsearchRequest request) {
		if ( request == null ) {
			return "(no request)";
		}

		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		StringBuilder sb = new StringBuilder( 180 );

		sb.append( request.method() )
				.append( " " )
				.append( request.path() )
				.append( " with parameters " )
				.append( request.parameters() );

		return sb.toString();
	}
}
