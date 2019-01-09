/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

/**
 * @author Guillaume Smet
 */
class LuceneLocalDateIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneLocalDateIndexFieldTypeContext, LocalDate> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneLocalDateIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext,
			LuceneIndexSchemaFieldDslBackReference<LocalDate> fieldDslBackReference) {
		super( buildContext, LocalDate.class, fieldDslBackReference );
	}

	@Override
	public LuceneLocalDateIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected LuceneIndexFieldType<LocalDate> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends LocalDate> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super LocalDate, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneLocalDateFieldCodec codec = new LuceneLocalDateFieldCodec( resolvedProjectable, resolvedSortable );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);
	}

	@Override
	protected LuceneLocalDateIndexFieldTypeContext thisAsS() {
		return this;
	}
}
