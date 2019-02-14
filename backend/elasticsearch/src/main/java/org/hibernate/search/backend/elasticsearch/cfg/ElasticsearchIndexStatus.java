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
import org.hibernate.search.util.impl.common.StringHelper;

public enum ElasticsearchIndexStatus {

	GREEN("green"),
	YELLOW("yellow"),
	RED("red");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchIndexStatus of(String value) {
		return StringHelper.parseDiscreteValues(
				ElasticsearchIndexStatus.values(),
				ElasticsearchIndexStatus::getElasticsearchString,
				log::invalidIndexStatus,
				value
		);
	}

	private final String externalRepresentation;

	ElasticsearchIndexStatus(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String getElasticsearchString() {
		return externalRepresentation;
	}
}
