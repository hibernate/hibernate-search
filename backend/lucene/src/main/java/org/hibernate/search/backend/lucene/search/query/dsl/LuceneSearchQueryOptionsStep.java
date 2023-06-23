/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.dsl;

import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchFetchable;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

public interface LuceneSearchQueryOptionsStep<H, LOS>
		extends SearchQueryOptionsStep<
				LuceneSearchQueryOptionsStep<H, LOS>,
				H,
				LOS,
				LuceneSearchSortFactory,
				LuceneSearchAggregationFactory>,
		LuceneSearchFetchable<H> {

	@Override
	LuceneSearchQuery<H> toQuery();

}
