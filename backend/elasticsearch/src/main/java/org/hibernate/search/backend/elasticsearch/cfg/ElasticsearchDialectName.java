/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum ElasticsearchDialectName {

	/**
	 * Use the dialect targeting Elasticsearch 5.6.*.
	 */
	ES_5_6("5.6"),
	/**
	 * Use the dialect targeting Elasticsearch 6.
	 */
	ES_6("6"),
	/**
	 * Use the dialect targeting Elasticsearch 7.
	 */
	ES_7("7"),
	/**
	 * Send a request to the cluster on bootstrap to detect the version automatically.
	 */
	AUTO("auto");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String externalRepresentation;

	ElasticsearchDialectName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	@Override
	public String toString() {
		return externalRepresentation;
	}

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchDialectName of(String value) {
		return StringHelper.parseDiscreteValues(
				ElasticsearchDialectName.values(),
				ElasticsearchDialectName::getExternalRepresentation,
				log::invalidDialectName,
				value
		);
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}
}
