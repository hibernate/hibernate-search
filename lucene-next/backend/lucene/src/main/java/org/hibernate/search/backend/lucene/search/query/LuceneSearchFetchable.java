/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchFetchable;

public interface LuceneSearchFetchable<H>
		extends ExtendedSearchFetchable<H, LuceneSearchResult<H>, LuceneSearchScroll<H>> {

}
