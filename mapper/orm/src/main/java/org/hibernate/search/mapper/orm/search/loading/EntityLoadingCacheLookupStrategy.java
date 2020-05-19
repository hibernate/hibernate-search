/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Strategy for cache lookup before search query results are loaded.
 * <p>
 * In most cases, no presence check is necessary.
 *
 * @author Emmanuel Bernard
 */
public enum EntityLoadingCacheLookupStrategy {

	/**
	 * When a search query returns entities,
	 * do not check any cache
	 * and load all the entities through an SQL query.
	 * <p>
	 * This is the default strategy.
	 */
	SKIP( "skip" ),

	/**
	 * When a search query returns entities,
	 * first check the persistence context to retrieve entities that are already in the session,
	 * then load the entities that were not found in the session through an SQL query.
	 */
	PERSISTENCE_CONTEXT( "persistence-context" ),

	/**
	 * When a search query returns entities,
	 * first check the persistence context to retrieve entities that are already in the session,
	 * then check the second level cache to retrieve entities that are in the 2LC but not in the session,
	 * then load the entities that were not found in the session or 2LC through an SQL query.
	 */
	PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE( "persistence-context-then-second-level-cache" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static EntityLoadingCacheLookupStrategy of(String value) {
		return StringHelper.parseDiscreteValues(
				EntityLoadingCacheLookupStrategy.values(),
				EntityLoadingCacheLookupStrategy::externalRepresentation,
				log::invalidAutomaticIndexingSynchronizationStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	EntityLoadingCacheLookupStrategy(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}

}
