package org.hibernate.search.backend.elasticsearch.search.query.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;

public interface ElasticsearchSearchQueryWhereStep<SR, H, LOS>
		extends
		SearchQueryWhereStep<SR,
				ElasticsearchSearchQueryOptionsStep<SR, H, LOS>,
				H,
				LOS,
				ElasticsearchSearchPredicateFactory<SR>> {

}
