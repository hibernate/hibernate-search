/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum AutomaticIndexingSynchronizationStrategyName {

	/**
	 * A strategy that only waits for indexing requests to be queued in the backend.
	 * <p>
	 * See the reference documentation for details.
	 */
	QUEUED( "queued" ),
	/**
	 * A strategy that waits for indexing requests to be committed.
	 * <p>
	 * See the reference documentation for details.
	 */
	COMMITTED( "committed" ),
	/**
	 * A strategy that waits for indexing requests to be committed and forces index refreshes.
	 * <p>
	 * See the reference documentation for details.
	 */
	SEARCHABLE( "searchable" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static AutomaticIndexingSynchronizationStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				AutomaticIndexingSynchronizationStrategyName.values(),
				AutomaticIndexingSynchronizationStrategyName::getExternalRepresentation,
				log::invalidAutomaticIndexingSynchronizationStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	AutomaticIndexingSynchronizationStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}

}
