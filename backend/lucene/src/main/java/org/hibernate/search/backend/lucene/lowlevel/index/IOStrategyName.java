/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.index;

import org.hibernate.search.backend.lucene.logging.impl.ConfigurationLog;
import org.hibernate.search.engine.cfg.spi.ParseUtils;

public enum IOStrategyName {

	/**
	 * The default, near-real-time strategy,
	 * where index readers are based on the index writer to get up-to-date search results,
	 * the index writer is committed periodically.
	 */
	NEAR_REAL_TIME( "near-real-time" ),
	/**
	 * A simple, low-performance strategy mainly useful for debugging,
	 * where index readers are created on each search query execution
	 * and the index writer is committed after each write.
	 */
	DEBUG( "debug" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static IOStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				IOStrategyName.values(),
				IOStrategyName::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidIOStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	IOStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	private String externalRepresentation() {
		return externalRepresentation;
	}
}
