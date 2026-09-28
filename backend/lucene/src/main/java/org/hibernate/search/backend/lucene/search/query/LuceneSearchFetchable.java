package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchFetchable;

public interface LuceneSearchFetchable<H>
		extends ExtendedSearchFetchable<H, LuceneSearchResult<H>, LuceneSearchScroll<H>> {

}
