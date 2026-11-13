package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display backend and transport requests in log messages.
 *
 */
public final class ElasticsearchRequestFormatter {

	private final String formatted;

	public ElasticsearchRequestFormatter(ElasticsearchJsonRequest request) {
		formatted = request == null ? "(no request)" : formatRequest( request.method(), request.path(), request.parameters() );
	}

	public ElasticsearchRequestFormatter(ElasticsearchRequest request) {
		formatted = request == null ? "(no request)" : formatRequest( request.method(), request.path(), request.parameters() );
	}

	@Override
	public String toString() {
		return formatted;
	}

	private static String formatRequest(String method, String path, Map<String, String> parameters) {
		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		StringBuilder sb = new StringBuilder( 180 );

		sb.append( method )
				.append( " " )
				.append( path )
				.append( " with parameters " )
				.append( parameters );

		return sb.toString();
	}
}
