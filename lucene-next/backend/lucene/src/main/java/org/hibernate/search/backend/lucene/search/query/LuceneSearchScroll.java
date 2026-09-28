package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchScroll;

public interface LuceneSearchScroll<H>
		extends ExtendedSearchScroll<H, LuceneSearchScrollResult<H>> {

}
