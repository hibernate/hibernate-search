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
import org.hibernate.search.util.impl.common.StringHelper;

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

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static HibernateOrmIndexingStrategyName parse(CharSequence charSequence) {
		return StringHelper.parseDiscreteValues(
				HibernateOrmIndexingStrategyName.values(),
				HibernateOrmIndexingStrategyName::getExternalRepresentation,
				log::invalidIndexingStrategyName,
				charSequence
		);
	}

	private final String externalRepresentation;

	HibernateOrmIndexingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}
}
