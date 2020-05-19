/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum IOStrategyName {

	/**
	 * The default, near-real-time strategy,
	 * where index readers are based on the index writer to get up-to-date search results,
	 * and the index writer is
	 */
	NEAR_REAL_TIME( "near-real-time" ),
	DEBUG( "debug" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static IOStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				IOStrategyName.values(),
				IOStrategyName::externalRepresentation,
				log::invalidIOStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	IOStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	private String externalRepresentation() {
		return externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 * @deprecated Use {@link #externalRepresentation()} instead.
	 */
	@Deprecated
	private String getExternalRepresentation() {
		return externalRepresentation();
	}
}
