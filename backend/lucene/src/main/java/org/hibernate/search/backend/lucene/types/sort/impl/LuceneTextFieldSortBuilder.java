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
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

public class LuceneTextFieldSortBuilder<F>
		extends AbstractLuceneStandardFieldSortBuilder<F, String, LuceneTextFieldCodec<F>> {

	LuceneTextFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneTextFieldCodec<F> codec) {
		super( searchContext, absoluteFieldPath, converter, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
	}

	@Override
	protected Object encodeMissingAs(F converted) {
		return codec.normalize( absoluteFieldPath, codec.encode( converted ) );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.STRING, order == SortOrder.DESC );
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}
}
