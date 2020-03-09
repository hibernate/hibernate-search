/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for dropping an indexes.
 *
 */
public interface ElasticsearchSchemaDropper {

	/**
	 * Drops an index, throwing an exception if dropping fails.
	 *
	 * <p>This method will skip operations silently if the index does not exist.
	 *
	 * @param indexNames The names of the index to drop.
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	CompletableFuture<?> dropIfExisting(IndexNames indexNames);

}
