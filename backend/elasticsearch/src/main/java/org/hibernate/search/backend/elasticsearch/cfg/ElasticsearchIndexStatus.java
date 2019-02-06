/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

public enum ElasticsearchIndexStatus {

	GREEN("green"),
	YELLOW("yellow"),
	RED("red");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String elasticsearchString;

	ElasticsearchIndexStatus(String elasticsearchString) {
		this.elasticsearchString = elasticsearchString;
	}

	public String getElasticsearchString() {
		return elasticsearchString;
	}

	public static ElasticsearchIndexStatus fromExternalRepresentation(String status) {
		for ( ElasticsearchIndexStatus indexStatus : ElasticsearchIndexStatus.values() ) {
			if ( indexStatus.getElasticsearchString().equalsIgnoreCase( status ) ) {
				return indexStatus;
			}
		}

		throw log.unexpectedIndexStatusString( status );
	}
}
