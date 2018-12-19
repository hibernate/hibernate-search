/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.SortField;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

public class LuceneNumericFieldSortBuilder<F, E>
		extends AbstractLuceneStandardFieldSortBuilder<F, E, LuceneNumericFieldCodec<F, E>> {

	LuceneNumericFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneNumericFieldCodec<F, E> codec) {
		super(
				searchContext, absoluteFieldPath,
				converter, codec,
				codec.getDomain().getMinValue(),
				codec.getDomain().getMaxValue()
		);
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		SortField sortField = new SortField(
				absoluteFieldPath,
				codec.getDomain().getSortFieldType(),
				order == SortOrder.DESC
		);
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}
}
