/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneTextFieldComparatorSource;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import org.apache.lucene.search.SortField;

public class LuceneTextFieldSortBuilder<F>
		extends AbstractLuceneStandardFieldSortBuilder<F, String, LuceneTextFieldCodec<F>> {

	LuceneTextFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, String nestedDocumentPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, LuceneTextFieldCodec<F> codec) {
		super( searchContext, absoluteFieldPath, nestedDocumentPath, converter, rawConverter, converterChecker, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
	}

	@Override
	protected Object encodeMissingAs(F converted) {
		return codec.normalize( absoluteFieldPath, codec.encode( converted ) );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		LuceneTextFieldComparatorSource fieldComparatorSource = new LuceneTextFieldComparatorSource( nestedDocumentPath, missingValue );
		SortField sortField = new SortField( absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );

		collector.collectSortField( sortField, ( nestedDocumentPath != null ) ? fieldComparatorSource : null );
	}
}
