/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.schema.management;

import java.util.Optional;

import org.hibernate.search.engine.common.schema.management.SchemaExport;

/**
 * A schema collector that walks through all schema exports of a schema manager this collector is passed to.
 */
public interface SearchSchemaCollector {

	/**
	 * Called when an {@link SchemaExport index schema export} is encountered.
	 *
	 * @param backendName The name of the index's backend, or {@link Optional#empty()} for the default backend.
	 * @param indexName The name of the index.
	 * @param export The index schema export from a schema manager this collector is passed to.
	 */
	void indexSchema(Optional<String> backendName, String indexName, SchemaExport export);
}
