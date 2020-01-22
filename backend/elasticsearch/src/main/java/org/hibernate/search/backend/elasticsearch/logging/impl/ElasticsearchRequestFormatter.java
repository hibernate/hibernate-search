/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

		sb.append( request.getMethod() )
				.append( " " )
				.append( request.getPath() )
				.append( " with parameters " )
				.append( request.getParameters() );

		return sb.toString();
	}
}
