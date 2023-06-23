/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Strategy for listener-triggered indexing in Hibernate Search.
 *
 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
 */
@Deprecated
public enum AutomaticIndexingStrategyName {

	/**
	 * No listener-triggered indexing is performed:
	 * indexing will only happen when explicitly requested through APIs
	 * such as {@link SearchSession#indexingPlan()}.
	 *
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
	 */
	@Deprecated
	NONE( "none" ),

	/**
	 * Indexing is triggered automatically when entities are modified in the Hibernate ORM session:
	 * entity insertion, update etc.
	 *
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
	 */
	@Deprecated
	SESSION( "session" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static AutomaticIndexingStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				AutomaticIndexingStrategyName.values(),
				AutomaticIndexingStrategyName::getExternalRepresentation,
				log::invalidAutomaticIndexingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	AutomaticIndexingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}
}
