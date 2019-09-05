/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.SortField;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.nested.onthefly.impl.NestedNumericFieldComparatorSource;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

public class LuceneNumericFieldSortBuilder<F, E extends Number>
		extends AbstractLuceneStandardFieldSortBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, String nestedDocumentPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, AbstractLuceneNumericFieldCodec<F, E> codec) {
		super(
				searchContext, absoluteFieldPath, nestedDocumentPath,
				converter, rawConverter, converterChecker, codec,
				codec.getDomain().getMinValue(),
				codec.getDomain().getMaxValue()
		);
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		collector.collectSortField( createSortField(), nestedFieldSort );
	}

	private SortField createSortField() {
		SortField.Type sortFieldType = codec.getDomain().getSortFieldType();
		if ( nestedDocumentPath != null ) {
			nestedFieldSort = new NestedNumericFieldComparatorSource( nestedDocumentPath, sortFieldType, getEffectiveMissingValue( missingValue, order ) );
			return new SortField( absoluteFieldPath, nestedFieldSort, order == SortOrder.DESC );
		}

		SortField sortField = new SortField(
				absoluteFieldPath,
				sortFieldType,
				order == SortOrder.DESC
		);
		setEffectiveMissingValue( sortField, missingValue, order );
		return sortField;
	}
}
