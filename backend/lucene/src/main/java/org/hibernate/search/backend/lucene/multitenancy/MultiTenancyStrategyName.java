/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.multitenancy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum MultiTenancyStrategyName {

	/**
	 * Single tenant configuration.
	 */
	NONE("none"),

	/**
	 * The multi-tenancy information is stored in the index as a discriminator field.
	 */
	DISCRIMINATOR("discriminator");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static MultiTenancyStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				MultiTenancyStrategyName.values(),
				MultiTenancyStrategyName::externalRepresentation,
				log::invalidMultiTenancyStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	MultiTenancyStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	private String externalRepresentation() {
		return externalRepresentation;
	}
}