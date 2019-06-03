/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;


import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Strategy for automatic indexing in Hibernate Search.
 */
public enum HibernateOrmAutomaticIndexingStrategyName {

	/**
	 * No automatic indexing is performed:
	 * indexing will only happen when explicitly requested through APIs
	 * such as {@link SearchSession#writePlan()}.
	 */
	NONE("none"),

	/**
	 * Indexing is triggered automatically when entities are modified in the Hibernate ORM session:
	 * entity insertion, update etc.
	 */
	SESSION("session");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static HibernateOrmAutomaticIndexingStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				HibernateOrmAutomaticIndexingStrategyName.values(),
				HibernateOrmAutomaticIndexingStrategyName::getExternalRepresentation,
				log::invalidAutomaticIndexingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	HibernateOrmAutomaticIndexingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}
}
