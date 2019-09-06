/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum ConfigurationPropertyCheckingStrategyName {

	/**
	 * Ignore the result of configuration property checking.
	 * Do not do anything if a Hibernate Search configuration property is set, but never used.
	 */
	IGNORE( "ignore" ),
	/**
	 * Log a warning if a Hibernate Search configuration property is set, but never used.
	 */
	WARN( "warn" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ConfigurationPropertyCheckingStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				ConfigurationPropertyCheckingStrategyName.values(),
				ConfigurationPropertyCheckingStrategyName::getExternalRepresentation,
				log::invalidConfigurationPropertyCheckingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	ConfigurationPropertyCheckingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String getExternalRepresentation() {
		return externalRepresentation;
	}

}
