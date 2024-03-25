/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneWithParametersAggregation;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;

public class LuceneSearchAggregationBuilderFactory implements SearchAggregationBuilderFactory {

	private final LuceneSearchIndexScope<?> scope;

	public LuceneSearchAggregationBuilderFactory(LuceneSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	@Override
	public <T> WithParametersAggregationBuilder<T> withParameters() {
		return new LuceneWithParametersAggregation.Builder<>( scope );
	}
}
