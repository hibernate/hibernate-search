/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query;

import org.hibernate.search.backend.lucene.search.dsl.aggregation.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchFetchable;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;

public interface LuceneSearchQueryOptionsStep<H>
		extends SearchQueryOptionsStep<
						LuceneSearchQueryOptionsStep<H>,
						H,
						LuceneSearchSortFactory,
						LuceneSearchAggregationFactory
				>,
				LuceneSearchFetchable<H> {

	@Override
	LuceneSearchQuery<H> toQuery();

}
