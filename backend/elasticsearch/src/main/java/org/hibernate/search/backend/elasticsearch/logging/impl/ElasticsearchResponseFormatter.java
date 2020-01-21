/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.JsonLogHelper;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link ElasticsearchResponse}s in log messages.
 *
 */
public final class ElasticsearchResponseFormatter {

	private final ElasticsearchResponse response;

	public ElasticsearchResponseFormatter(ElasticsearchResponse response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return formatResponse( response );
	}

	private static String formatResponse(ElasticsearchResponse response) {
		if ( response == null ) {
			return "(no response)";
		}

		JsonLogHelper helper = JsonLogHelper.get();

		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		//Also useful to hint the builder to use larger increment steps.
		StringBuilder sb = new StringBuilder( 180 );
		sb.append( response.getStatusCode() )
				.append( " '" )
				.append( response.getStatusMessage() )
				.append( "' with body " )
				.append( helper.toString( response.getBody() ) );

		return sb.toString();
	}
}
