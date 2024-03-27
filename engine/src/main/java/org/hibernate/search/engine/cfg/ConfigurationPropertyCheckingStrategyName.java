/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.logging.impl.Log;
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
		return ParseUtils.parseDiscreteValues(
				ConfigurationPropertyCheckingStrategyName.values(),
				ConfigurationPropertyCheckingStrategyName::externalRepresentation,
				log::invalidConfigurationPropertyCheckingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	ConfigurationPropertyCheckingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
