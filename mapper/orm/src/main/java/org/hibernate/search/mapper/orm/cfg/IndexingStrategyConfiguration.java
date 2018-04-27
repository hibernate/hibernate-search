/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;


import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Modes for triggering indexing of entities.
 *
 * @author Gunnar Morling
 */
public enum IndexingStrategyConfiguration {

	/**
	 * Indexing is triggered automatically upon entity insertion, update etc.
	 */
	EVENT("event"),

	/**
	 * Indexing is triggered explicitly.
	 */
	MANUAL("manual");

	private static Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String externalRepresentation;

	private IndexingStrategyConfiguration(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * Returns the {@link IndexingStrategyConfiguration} matching the given external representation as specified via
	 * {@link SearchOrmSettings#INDEXING_STRATEGY}
	 * @param indexingMode the indexing mode external representation
	 * @return the {@link IndexingStrategyConfiguration}
	 */
	public static IndexingStrategyConfiguration fromExternalRepresentation(String indexingMode) {
		if ( EVENT.toExternalRepresentation().equals( indexingMode ) ) {
			return IndexingStrategyConfiguration.EVENT;
		}
		else if ( MANUAL.toExternalRepresentation().equals( indexingMode ) ) {
			return IndexingStrategyConfiguration.MANUAL;
		}
		else {
			throw LOG.unknownIndexingMode( indexingMode );
		}
	}

	/**
	 * Returns the external representation of this indexing mode. Generally this enumeration itself should preferably be
	 * used for comparisons etc.
	 * @return the external representation as string
	 */
	public String toExternalRepresentation() {
		return externalRepresentation;
	}
}
