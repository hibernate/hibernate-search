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
import org.hibernate.search.backend.lucene.types.sort.missing.impl.LuceneMissingValueComparatorSource;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

public class LuceneTextFieldSortBuilder<F>
		extends AbstractLuceneStandardFieldSortBuilder<F, String, LuceneTextFieldCodec<F>> {

	LuceneTextFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, LuceneTextFieldCodec<F> codec) {
		super( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
	}

	@Override
	protected Object encodeMissingAs(F converted) {
		return codec.normalize( absoluteFieldPath, codec.encode( converted ) );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		// For STRING type, missing value must be either STRING_FIRST or STRING_LAST.
		// Otherwise we need a CUSTOM type.
		collector.collectSortField( ( useMissingValue() ) ? customType() : stringType() );
	}

	private boolean useMissingValue() {
		return missingValue != null && !SortMissingValue.MISSING_FIRST.equals( missingValue ) && !SortMissingValue.MISSING_LAST.equals( missingValue );
	}

	private SortField stringType() {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.STRING, order == SortOrder.DESC );
		setEffectiveMissingValue( sortField, missingValue, order );
		return sortField;
	}

	public SortField customType() {
		return new SortField( absoluteFieldPath, new LuceneMissingValueComparatorSource( (BytesRef) missingValue ), order == SortOrder.DESC );
	}
}
