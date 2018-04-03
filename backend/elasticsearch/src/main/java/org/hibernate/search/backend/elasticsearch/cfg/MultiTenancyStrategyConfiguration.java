/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

public enum MultiTenancyStrategyConfiguration {

	/**
	 * Single tenant configuration.
	 */
	NONE("none"),

	/**
	 * The multi-tenancy information is stored in the index as a discriminator field.
	 */
	DISCRIMINATOR("discriminator");

	private static Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String externalRepresentation;

	private MultiTenancyStrategyConfiguration(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public static MultiTenancyStrategyConfiguration fromExternalRepresentation(String multiTenancyStrategy) {
		if ( NONE.externalRepresentation.equals( multiTenancyStrategy ) ) {
			return NONE;
		}
		else if ( DISCRIMINATOR.externalRepresentation.equals( multiTenancyStrategy ) ) {
			return DISCRIMINATOR;
		}
		else {
			throw LOG.unknownMultiTenancyStrategyConfiguration( multiTenancyStrategy );
		}
	}
}
