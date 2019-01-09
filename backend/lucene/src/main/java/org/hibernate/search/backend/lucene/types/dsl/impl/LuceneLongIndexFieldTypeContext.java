/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLongFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneLongIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneLongIndexFieldTypeContext, Long> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneLongIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext,
			LuceneIndexSchemaFieldDslBackReference<Long> fieldDslBackReference) {
		super( buildContext, Long.class, fieldDslBackReference );
	}

	@Override
	public LuceneLongIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected LuceneIndexFieldType<Long> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends Long> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Long, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneLongFieldCodec codec = new LuceneLongFieldCodec( resolvedProjectable, resolvedSortable );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);
	}

	@Override
	protected LuceneLongIndexFieldTypeContext thisAsS() {
		return this;
	}
}
