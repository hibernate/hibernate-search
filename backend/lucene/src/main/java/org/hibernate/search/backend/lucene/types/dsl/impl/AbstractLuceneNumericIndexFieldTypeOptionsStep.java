/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

abstract class AbstractLuceneNumericIndexFieldTypeOptionsStep<S extends AbstractLuceneNumericIndexFieldTypeOptionsStep<S, F>, F>
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<S, F> {

	private Sortable sortable = Sortable.DEFAULT;

	AbstractLuceneNumericIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType) {
		super( buildContext, fieldType );
	}

	@Override
	public S sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public LuceneIndexFieldType<F> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter =
				createDslToIndexConverter();
		ToDocumentFieldValueConverter<F, ? extends F> rawDslToIndexConverter =
				createToDocumentRawConverter();
		FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		FromDocumentFieldValueConverter<? super F, F> rawIndexToProjectionConverter =
				createFromDocumentRawConverter();
		AbstractLuceneNumericFieldCodec<F, ?> codec = createCodec(
				resolvedProjectable,
				resolvedSearchable,
				resolvedSortable,
				indexNullAsValue
		);

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>(
						resolvedSearchable, dslToIndexConverter, rawDslToIndexConverter, codec
				),
				new LuceneNumericFieldSortBuilderFactory<>(
						resolvedSortable, dslToIndexConverter, rawDslToIndexConverter, codec
				),
				new LuceneStandardFieldProjectionBuilderFactory<>(
						resolvedProjectable, indexToProjectionConverter, rawIndexToProjectionConverter, codec
				)
		);
	}

	protected abstract AbstractLuceneNumericFieldCodec<F, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable,
			F indexNullAsValue);

}
