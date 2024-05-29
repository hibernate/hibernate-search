/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static MassIndexingDefaultCleanOperation of(String value) {
		return ParseUtils.parseDiscreteValues(
				MassIndexingDefaultCleanOperation.values(),
				MassIndexingDefaultCleanOperation::externalRepresentation,
				log::invalidMassIndexingDefaultCleanOperation,
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
