/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link ElasticsearchRequest}s in log messages.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchRequestFormatter {

	private final String stringRepresentation;

	public ElasticsearchRequestFormatter(ElasticsearchRequest request) {
		this.stringRepresentation = formatRequest( request );
	}

	private static String formatRequest(ElasticsearchRequest request) {
		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		StringBuilder sb = new StringBuilder( 180 );

		sb.append( "Method: " ).append( request.getMethod() );
		sb.append( "\nPath: " ).append( request.getPath() );
		sb.append( "\nData:\n" );
		JsonLogHelper.get().append( sb, request.getBodyParts() );
		sb.append( "\n\n" );

		return sb.toString();
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
