/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.multitenancy;

import org.hibernate.search.backend.lucene.logging.impl.ConfigurationLog;
import org.hibernate.search.engine.cfg.spi.ParseUtils;

public enum MultiTenancyStrategyName {

	/**
	 * Single tenant configuration.
	 */
	NONE( "none" ),

	/**
	 * The multi-tenancy information is stored in the index as a discriminator field.
	 */
	DISCRIMINATOR( "discriminator" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static MultiTenancyStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				MultiTenancyStrategyName.values(),
				MultiTenancyStrategyName::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidMultiTenancyStrategyName,
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
