/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.OffsetDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneOffsetDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneOffsetDateTimeIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneOffsetDateTimeIndexFieldTypeContext, OffsetDateTime> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneOffsetDateTimeIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class );
	}

	@Override
	public LuceneOffsetDateTimeIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<OffsetDateTime> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends OffsetDateTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super OffsetDateTime, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneOffsetDateTimeFieldCodec codec = new LuceneOffsetDateTimeFieldCodec( resolvedProjectable, resolvedSortable );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);
	}

	@Override
	protected LuceneOffsetDateTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
