package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchScroll;

public interface ElasticsearchSearchScroll<H>
		extends ExtendedSearchScroll<H, ElasticsearchSearchScrollResult<H>> {

}
