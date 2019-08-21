/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.aggregation.impl;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.search.dsl.aggregation.LuceneSearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.aggregation.spi.DelegatingSearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.aggregation.spi.SearchAggregationDslContext;

public class LuceneSearchAggregationFactoryImpl
		extends DelegatingSearchAggregationFactory
		implements LuceneSearchAggregationFactory {

	private final SearchAggregationDslContext<LuceneSearchAggregationBuilderFactory> dslContext;

	public LuceneSearchAggregationFactoryImpl(SearchAggregationFactory delegate,
			SearchAggregationDslContext<LuceneSearchAggregationBuilderFactory> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	// Empty: no extension at the moment.

}
