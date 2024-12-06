/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.SearchResult;

import org.apache.lucene.search.TopDocs;

public interface LuceneSearchResult<H> extends SearchResult<H> {

	/**
	 * @return the Lucene topDocs produced by the query.
	 * <p>
	 * For the common use cases there is no meaning for using it.
	 * It might be used by an advanced user who needs to merge different query result,
	 * using the merge low level Lucene API, such as {@link TopDocs#merge(int, TopDocs[])}.
	 */
	TopDocs topDocs();

}
