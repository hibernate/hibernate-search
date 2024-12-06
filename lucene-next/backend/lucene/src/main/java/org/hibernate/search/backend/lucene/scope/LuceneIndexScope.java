/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.scope;

import java.util.Collections;
import java.util.Set;

import org.apache.lucene.index.IndexReader;

/**
 * The {@code LuceneIndexScope} exposes {@link IndexReader}s directly, making it possible to query the Lucene
 * indexes directly bypassing Hibernate Search.
 * <p>
 * The returned IndexReader instances must be closed
 * using the {@link IndexReader#close()} method.
 * <p>
 * This API is intended for power users intending to extract information directly.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface LuceneIndexScope {

	/**
	 * Opens an IndexReader for all indexes containing the entities of the search scope.
	 * <p>
	 * The instance must be closed after its use by the client of this class using {@link IndexReader#close()}.
	 * <p>
	 * If sharding is enabled, the returned reader will read all shards.
	 * If you need to read specific shards only, use {@link #openIndexReader(Set)}.
	 * <p>
	 * <strong>WARNING:</strong> Even if multi-tenancy is enabled, the returned reader exposes documents of *all* tenants.
	 *
	 * @return an IndexReader containing the entities of the index scope
	 */
	default IndexReader openIndexReader() {
		return openIndexReader( Collections.emptySet() );
	}

	/**
	 * Opens an IndexReader on shards assigned to the given routing keys
	 * for all indexes containing the entities of the search scope.
	 * <p>
	 * The instance must be closed after its use by the client of this class using {@link IndexReader#close()}.
	 * <p>
	 * <strong>WARNING:</strong> Even if multi-tenancy is enabled, the returned reader exposes documents of *all* tenants.
	 *
	 * @param routingKeys A collection containing zero, one or multiple string keys.
	 * @return an IndexReader containing the entities of the index scope
	 */
	IndexReader openIndexReader(Set<String> routingKeys);

}
