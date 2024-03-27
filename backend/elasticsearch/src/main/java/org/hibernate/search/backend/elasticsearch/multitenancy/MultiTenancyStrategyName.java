/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.multitenancy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum MultiTenancyStrategyName {

	/**
	 * Single tenant configuration.
	 */
	NONE( "none" ),

	/**
	 * The multi-tenancy information is stored in the index as a discriminator field.
	 */
	DISCRIMINATOR( "discriminator" );

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
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
