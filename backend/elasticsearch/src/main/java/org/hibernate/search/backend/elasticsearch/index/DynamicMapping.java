/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index;

import org.hibernate.search.backend.elasticsearch.logging.spi.ConfigurationLog;
import org.hibernate.search.engine.cfg.spi.ParseUtils;

public enum DynamicMapping {

	/**
	 * Add unknown fields to the schema dynamically
	 */
	TRUE( "true" ),

	/**
	 * Ignore unknown fields
	 */
	FALSE( "false" ),

	/**
	 * Throw an exception on unknown fields
	 */
	STRICT( "strict" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static DynamicMapping of(String value) {
		return ParseUtils.parseDiscreteValues(
				DynamicMapping.values(),
				DynamicMapping::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidDynamicType,
				value
		);
	}

	private final String externalRepresentation;

	DynamicMapping(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String externalRepresentation() {
		return externalRepresentation;
	}
}
