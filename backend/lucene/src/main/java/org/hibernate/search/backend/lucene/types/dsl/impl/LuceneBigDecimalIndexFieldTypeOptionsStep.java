/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBigDecimalFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneBigDecimalIndexFieldTypeOptionsStep
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<LuceneBigDecimalIndexFieldTypeOptionsStep, BigDecimal>
		implements ScaledNumberIndexFieldTypeOptionsStep<LuceneBigDecimalIndexFieldTypeOptionsStep, BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Sortable sortable = Sortable.DEFAULT;
	private Integer decimalScale = null;

	LuceneBigDecimalIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigDecimal.class );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public LuceneBigDecimalIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneBigDecimalIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return this;
	}

	@Override
	public LuceneIndexFieldType<BigDecimal> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		ToDocumentFieldValueConverter<?, ? extends BigDecimal> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super BigDecimal, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneBigDecimalFieldCodec codec = new LuceneBigDecimalFieldCodec( resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue, resolveDecimalScale() );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec )
		);
	}

	@Override
	protected LuceneBigDecimalIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.getDecimalScale() != null ) {
			return defaultsProvider.getDecimalScale();
		}

		throw log.nullDecimalScale( getBuildContext().getEventContext() );
	}
}
