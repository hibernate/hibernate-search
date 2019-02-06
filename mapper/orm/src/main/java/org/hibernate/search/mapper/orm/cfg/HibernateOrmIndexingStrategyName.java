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
public enum HibernateOrmIndexingStrategyName {

	/**
	 * Indexing is triggered automatically upon entity insertion, update etc.
	 */
	EVENT("event"),

	/**
	 * Indexing is triggered explicitly.
	 */
	MANUAL("manual");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String externalRepresentation;

	HibernateOrmIndexingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * Returns the {@link HibernateOrmIndexingStrategyName} matching the given external representation as specified via
	 * {@link HibernateOrmMapperSettings#INDEXING_STRATEGY}
	 * @param indexingMode the indexing mode external representation
	 * @return the {@link HibernateOrmIndexingStrategyName}
	 */
	public static HibernateOrmIndexingStrategyName fromExternalRepresentation(String indexingMode) {
		if ( EVENT.toExternalRepresentation().equals( indexingMode ) ) {
			return HibernateOrmIndexingStrategyName.EVENT;
		}
		else if ( MANUAL.toExternalRepresentation().equals( indexingMode ) ) {
			return HibernateOrmIndexingStrategyName.MANUAL;
		}
		else {
			throw log.unknownIndexingMode( indexingMode );
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
