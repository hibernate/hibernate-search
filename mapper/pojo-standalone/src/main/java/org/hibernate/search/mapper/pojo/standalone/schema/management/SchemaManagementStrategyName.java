/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.schema.management;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public enum SchemaManagementStrategyName {

	/**
	 * A strategy that does not do anything on startup or shutdown.
	 * <p>
	 * Indexes and their schema will not be created nor deleted on startup or shutdown.
	 * Hibernate Search will not even check that the index actually exists.
	 * <p>
	 * With Elasticsearch, indexes and their schema will have to be created explicitly.
	 */
	NONE( "none" ),

	/**
	 * A strategy that does not change indexes nor their schema,
	 * but checks that indexes exist and validates their schema on startup.
	 * <p>
	 * An exception will be thrown on startup if:
	 * <ul>
	 *     <li>Indexes are missing</li>
	 *     <li>OR, with Elasticsearch only, indexes exist but their schema does not match the requirements
	 *     of the Hibernate Search mapping:
	 *     missing fields, fields with incorrect type, missing analyzer definitions or normalizer definitions, ...
	 *     </li>
	 * </ul>
	 * <p>
	 * <strong>Warning:</strong> with the Lucene backend, validation is limited to checking that the indexes exist,
	 * because local Lucene indexes don't have a schema.
	 */
	VALIDATE( "validate" ),

	/**
	 * A strategy that creates missing indexes and their schema on startup,
	 * but does not touch existing indexes and assumes their schema is correct without validating it.
	 * <p>
	 * Note that creating indexes and their schema will not populate the indexed data:
	 * a newly created index will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 */
	CREATE( "create" ),

	/**
	 * A strategy that creates missing indexes and their schema on startup,
	 * and validates the schema of existing indexes.
	 * <p>
	 * Note that creating indexes and their schema will not populate the indexed data:
	 * newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 * <p>
	 * With Elasticsearch only, an exception will be thrown on startup if some indexes already exist
	 * but their schema does not match the requirements of the Hibernate Search mapping:
	 * missing fields, fields with incorrect type, missing analyzer definitions or normalizer definitions, ...
	 * <p>
	 * <strong>Warning:</strong> with the Lucene backend, validation is limited to checking that the indexes exist,
	 * because local Lucene indexes don't have a schema.
	 */
	CREATE_OR_VALIDATE( "create-or-validate" ),

	/**
	 * A strategy that creates missing indexes and their schema on startup,
	 * and updates the schema of existing indexes if possible.
	 * <p>
	 * Note that creating indexes or updating their schema will not populate or update the indexed data:
	 * newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 * <p>
	 * <strong>Note:</strong> with the Lucene backend, schema update is a no-op,
	 * because local Lucene indexes don't have a schema.
	 * <p>
	 * <strong>Warning:</strong> This strategy is unfit for production environments,
	 * due to the limitations explained below.
	 * It should only be relied upon during development,
	 * to easily add new fields to an existing index.
	 * <p>
	 * <strong>Warning:</strong> with the Elasticsearch backend, if analyzer/normalizer definitions have to be updated,
	 * the index will be closed automatically during the update.
	 * <p>
	 * <strong>Warning:</strong> with the Elasticsearch backend, many scenarios can cause schema updates to fail:
	 * a field changed its type from string to integer, an analyzer definition changed, ...
	 * In such cases, the only workaround is to drop and re-create the index.
	 */
	CREATE_OR_UPDATE( "create-or-update" ),

	/**
	 * A strategy that drops existing indexes and re-creates them and their schema on startup.
	 * <p>
	 * Note that dropping indexes means losing all indexed data,
	 * and creating indexes will not populate them:
	 * the newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 */
	DROP_AND_CREATE( "drop-and-create" ),

	/**
	 * A strategy that drops existing indexes and re-creates them and their schema on startup,
	 * then drops the indexes on shutdown.
	 * <p>
	 * Note that dropping indexes means losing all indexed data,
	 * and creating indexes will not populate them:
	 * the newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 */
	DROP_AND_CREATE_AND_DROP( "drop-and-create-and-drop" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static SchemaManagementStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				SchemaManagementStrategyName.values(),
				SchemaManagementStrategyName::externalRepresentation,
				log::invalidSchemaManagementStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	SchemaManagementStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
