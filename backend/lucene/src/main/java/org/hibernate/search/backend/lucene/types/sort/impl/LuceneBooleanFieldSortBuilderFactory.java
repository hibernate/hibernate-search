/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class LuceneBooleanFieldSortBuilderFactory
		extends AbstractLuceneStandardFieldSortBuilderFactory<Boolean, LuceneStandardFieldCodec<Boolean, ?>> {

	public LuceneBooleanFieldSortBuilderFactory(boolean sortable,
			ToDocumentFieldValueConverter<?, ? extends Boolean> converter,
			LuceneStandardFieldCodec<Boolean, ?> codec) {
		super( sortable, converter, codec );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		checkSortable( absoluteFieldPath );

		return new LuceneBooleanFieldSortBuilder( searchContext, absoluteFieldPath, converter, codec );
	}
}
