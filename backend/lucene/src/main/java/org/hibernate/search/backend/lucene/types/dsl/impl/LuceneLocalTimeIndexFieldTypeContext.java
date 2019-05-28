/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalTime;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneLocalTimeIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneLocalTimeIndexFieldTypeContext, LocalTime> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneLocalTimeIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class );
	}

	@Override
	public LuceneLocalTimeIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<LocalTime> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		ToDocumentFieldValueConverter<?, ? extends LocalTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super LocalTime, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneLocalTimeFieldCodec codec = new LuceneLocalTimeFieldCodec( resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec )
		);
	}

	@Override
	protected LuceneLocalTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
