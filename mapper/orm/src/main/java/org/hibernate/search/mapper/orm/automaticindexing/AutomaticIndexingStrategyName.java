/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.logging.impl.ConfigurationLog;
import org.hibernate.search.mapper.orm.session.SearchSession;

/**
 * Strategy for listener-triggered indexing in Hibernate Search.
 *
 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
 */
@Deprecated(since = "6.1")
public enum AutomaticIndexingStrategyName {

	/**
	 * No listener-triggered indexing is performed:
	 * indexing will only happen when explicitly requested through APIs
	 * such as {@link SearchSession#indexingPlan()}.
	 *
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
	 */
	@Deprecated( since = "6.1")
	NONE("none" ),

	/**
	 * Indexing is triggered automatically when entities are modified in the Hibernate ORM session:
	 * entity insertion, update etc.
	 *
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED} instead.
	 */
	@Deprecated( since = "6.1")
	SESSION("session" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static AutomaticIndexingStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				AutomaticIndexingStrategyName.values(),
				AutomaticIndexingStrategyName::getExternalRepresentation,
				ConfigurationLog.INSTANCE::invalidAutomaticIndexingStrategyName,
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
