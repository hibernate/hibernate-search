/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

public enum ElasticsearchIndexStatus {

	GREEN("green"),
	YELLOW("yellow"),
	RED("red");

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final String elasticsearchString;

	private ElasticsearchIndexStatus(String elasticsearchString) {
		this.elasticsearchString = elasticsearchString;
	}

	public String getElasticsearchString() {
		return elasticsearchString;
	}

	public static ElasticsearchIndexStatus fromString(String status) {
		for ( ElasticsearchIndexStatus indexStatus : ElasticsearchIndexStatus.values() ) {
			if ( indexStatus.getElasticsearchString().equalsIgnoreCase( status ) ) {
				return indexStatus;
			}
		}

		throw LOG.unexpectedIndexStatusString( status );
	}
}