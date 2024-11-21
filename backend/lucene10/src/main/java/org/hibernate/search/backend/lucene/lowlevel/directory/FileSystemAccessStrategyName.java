/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory;

import org.hibernate.search.backend.lucene.logging.impl.ConfigurationLog;
import org.hibernate.search.engine.cfg.spi.ParseUtils;

public enum FileSystemAccessStrategyName {

	AUTO( "auto" ),
	NIO( "nio" ),
	MMAP( "mmap" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static FileSystemAccessStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				FileSystemAccessStrategyName.values(),
				FileSystemAccessStrategyName::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidFileSystemAccessStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	FileSystemAccessStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
