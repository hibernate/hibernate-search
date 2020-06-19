/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneNumericFieldComparatorSource;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import org.apache.lucene.search.SortField;

public class LuceneNumericFieldSortBuilder<F, E extends Number>
	extends AbstractLuceneStandardFieldSortBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericFieldSortBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, field, codec, codec.getDomain().getMinValue(), codec.getDomain().getMaxValue() );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {

		LuceneNumericFieldComparatorSource<E> fieldComparatorSource = new LuceneNumericFieldComparatorSource<>(
				nestedDocumentPath, codec.getDomain(), (E) getEffectiveMissingValue( missingValue, order ), getMultiValueMode(), getNestedFilter() );
		SortField sortField = new SortField( absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );

		collector.collectSortField( sortField, (nestedDocumentPath != null) ? fieldComparatorSource : null );
	}

}
