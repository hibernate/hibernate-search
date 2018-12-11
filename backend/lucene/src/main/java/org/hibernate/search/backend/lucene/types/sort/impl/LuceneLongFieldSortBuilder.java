/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

import org.apache.lucene.search.SortField;

public class LuceneLongFieldSortBuilder
		extends AbstractLuceneStandardFieldSortBuilder<Long, LuceneStandardFieldCodec<Long, ?>> {

	LuceneLongFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends Long> converter,
			LuceneStandardFieldCodec<Long, ?> codec) {
		super( searchContext, absoluteFieldPath, converter, codec, Long.MIN_VALUE, Long.MAX_VALUE );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.LONG,
				order == SortOrder.DESC );
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}
}
