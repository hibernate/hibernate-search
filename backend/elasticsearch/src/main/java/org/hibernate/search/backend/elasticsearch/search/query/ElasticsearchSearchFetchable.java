package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchFetchable;

public interface ElasticsearchSearchFetchable<H>
		extends ExtendedSearchFetchable<H, ElasticsearchSearchResult<H>, ElasticsearchSearchScroll<H>> {

}
