/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope;

import java.util.Collections;
import java.util.Set;

import org.apache.lucene.index.IndexReader;

/**
 * The {@code LuceneIndexScope} exposes {@link IndexReader}s directly, making it possible to query the Lucene
 * indexes directly bypassing Hibernate Search.
 * <p>
 * The returned IndexReader instances are always read-only and must be closed
 * using the {@link IndexReader#close()} method.
 * <p>
 * This API is intended for power users intending to extract information directly.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface LuceneIndexScope {

	/**
	 * Opens an IndexReader on all indexes containing the entities of the search scope.
	 * In the simplest case if the scope has one entity, it will map to a single index; if the entity
	 * uses a sharding strategy or if multiple entities using different index names are selected,
	 * the single IndexReader will act as a MultiReader on the aggregate of these indexes.
	 * This MultiReader is not filtered by Hibernate Search, so it might contain information
	 * relevant to different types as well.
	 * <p>
	 * The returned IndexReader is read only; writing directly to the index is discouraged.
	 * <p>
	 * The instance must be closed after its use by the client of this class using {@link IndexReader#close()}.
	 * <p>
	 * By default, if routing is not configured, all shards will be queried.
	 * If you need to filter by routing keys use {@link #openIndexReader(Set)}.
	 * This api bypasses the filtering by tenantId, so results from all tenants will be always visible.
	 *
	 * @return an IndexReader containing the entities of the index scope
	 */
	default IndexReader openIndexReader() {
		return openIndexReader( Collections.emptySet() );
	}

	/**
	 * Opens an IndexReader on all indexes containing the entities of the search scope.
	 * In the simplest case if the scope has one entity, it will map to a single index; if the entity
	 * uses a sharding strategy or if multiple entities using different index names are selected,
	 * the single IndexReader will act as a MultiReader on the aggregate of these indexes.
	 * This MultiReader is not filtered by Hibernate Search, so it might contain information
	 * relevant to different types as well.
	 * <p>
	 * The returned IndexReader is read only; writing directly to the index is discouraged.
	 * <p>
	 * The instance must be closed after its use by the client of this class using {@link IndexReader#close()}.
	 * <p>
	 * Only the shards having keys contained in {@code routingKeys} will be visible to the reader.
	 * This api bypasses the filtering by tenantId, so results from all tenants will be always visible.
	 *
	 * @param routingKeys A collection containing zero, one or multiple string keys.
	 * @return an IndexReader containing the entities of the index scope
	 */
	IndexReader openIndexReader(Set<String> routingKeys);

}
