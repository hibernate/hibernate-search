/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.schema.management.spi;

import java.util.Optional;

import org.hibernate.search.engine.common.schema.management.SchemaExport;

/**
 * An index schema collector that walks through an index schema exports of a schema manager this collector is passed to.
 */
public interface IndexSchemaCollector {

	/**
	 * Called when an {@link SchemaExport index schema export} is encountered.
	 *
	 * @param backendName The name of the index's backend, or {@link Optional#empty()} for the default backend.
	 * @param indexName The name of the index.
	 * @param export The index schema export from a schema manager this collector is passed to.
	 */
	void indexSchema(Optional<String> backendName, String indexName, SchemaExport export);
}
