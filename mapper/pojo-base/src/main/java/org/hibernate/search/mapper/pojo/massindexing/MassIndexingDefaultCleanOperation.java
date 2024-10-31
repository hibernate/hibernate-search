/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.logging.impl.MassIndexingLog;

public enum MassIndexingDefaultCleanOperation {

	/**
	 * Removes all entities from the indexes before indexing.
	 *
	 * @see org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer#purgeAllOnStart(boolean)
	 */
	PURGE( "purge" ),
	/**
	 * Drops the indexes and their schema (if they exist) and re-creates them before indexing.
	 *
	 * @see org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer#dropAndCreateSchemaOnStart(boolean)
	 */
	DROP_AND_CREATE( "drop_and_create" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static MassIndexingDefaultCleanOperation of(String value) {
		return ParseUtils.parseDiscreteValues(
				MassIndexingDefaultCleanOperation.values(),
				MassIndexingDefaultCleanOperation::externalRepresentation,
				MassIndexingLog.INSTANCE::invalidMassIndexingDefaultCleanOperation,
				value
		);
	}

	private final String externalRepresentation;

	MassIndexingDefaultCleanOperation(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}

}
