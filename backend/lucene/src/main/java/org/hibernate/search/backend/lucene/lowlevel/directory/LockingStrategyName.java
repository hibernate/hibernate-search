/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum LockingStrategyName {

	SIMPLE_FILESYSTEM( "simple-filesystem" ),
	NATIVE_FILESYSTEM( "native-filesystem" ),
	SINGLE_INSTANCE( "single-instance" ),
	NONE( "none" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static LockingStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				LockingStrategyName.values(),
				LockingStrategyName::externalRepresentation,
				log::invalidLockingStrategyName,
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
