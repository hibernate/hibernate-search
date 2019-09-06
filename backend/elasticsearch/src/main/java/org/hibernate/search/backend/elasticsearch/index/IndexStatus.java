/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum IndexStatus {

	GREEN("green"),
	YELLOW("yellow"),
	RED("red");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static IndexStatus of(String value) {
		return StringHelper.parseDiscreteValues(
				IndexStatus.values(),
				IndexStatus::getElasticsearchString,
				log::invalidIndexStatus,
				value
		);
	}

	private final String externalRepresentation;

	IndexStatus(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String getElasticsearchString() {
		return externalRepresentation;
	}
}
