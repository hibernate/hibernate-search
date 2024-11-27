/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory;

import org.hibernate.search.backend.lucene.logging.impl.ConfigurationLog;
import org.hibernate.search.engine.cfg.spi.ParseUtils;

public enum LockingStrategyName {

	SIMPLE_FILESYSTEM( "simple-filesystem" ),
	NATIVE_FILESYSTEM( "native-filesystem" ),
	SINGLE_INSTANCE( "single-instance" ),
	NONE( "none" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static LockingStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				LockingStrategyName.values(),
				LockingStrategyName::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidLockingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	LockingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	private String externalRepresentation() {
		return externalRepresentation;
	}
}
