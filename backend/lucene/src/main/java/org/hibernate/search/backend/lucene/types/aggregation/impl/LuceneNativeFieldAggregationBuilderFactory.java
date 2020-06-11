/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneNativeFieldAggregationBuilderFactory<F> implements LuceneFieldAggregationBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isAggregable() {
		return false;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldAggregationBuilderFactory<?> other) {
		return getClass().equals( other.getClass() );
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		throw unsupported( field );
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		throw unsupported( field );
	}

	private SearchException unsupported(LuceneSearchFieldContext<?> field) {
		return log.unsupportedDSLAggregationsForNativeField( field.eventContext() );
	}
}
