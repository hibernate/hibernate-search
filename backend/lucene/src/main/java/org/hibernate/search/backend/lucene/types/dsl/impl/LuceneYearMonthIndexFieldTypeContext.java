/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.YearMonth;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneYearMonthFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneYearMonthIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneYearMonthIndexFieldTypeContext, YearMonth> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneYearMonthIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class );
	}

	@Override
	public LuceneYearMonthIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<YearMonth> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		ToDocumentFieldValueConverter<?, ? extends YearMonth> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super YearMonth, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneYearMonthFieldCodec codec = new LuceneYearMonthFieldCodec( resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec )
		);
	}

	@Override
	protected LuceneYearMonthIndexFieldTypeContext thisAsS() {
		return this;
	}
}
