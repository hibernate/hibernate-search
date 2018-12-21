/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneBooleanIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneBooleanIndexFieldTypeContext, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneBooleanIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext,
			LuceneIndexSchemaFieldDslBackReference<Boolean> fieldDslBackReference) {
		super( buildContext, Boolean.class, fieldDslBackReference );
	}

	@Override
	public LuceneBooleanIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected LuceneIndexFieldType<Boolean> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends Boolean> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Boolean, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneBooleanFieldCodec codec = new LuceneBooleanFieldCodec( resolvedProjectable, resolvedSortable );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);
	}

	@Override
	protected LuceneBooleanIndexFieldTypeContext thisAsS() {
		return this;
	}
}
