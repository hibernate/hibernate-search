/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBigIntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneBigIntegerIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneBigIntegerIndexFieldTypeContext, BigInteger>
		implements ScaledNumberIndexFieldTypeContext<LuceneBigIntegerIndexFieldTypeContext, BigInteger> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Sortable sortable = Sortable.DEFAULT;
	private Integer decimalScale = null;

	LuceneBigIntegerIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigInteger.class );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public LuceneBigIntegerIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneBigIntegerIndexFieldTypeContext decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return this;
	}

	@Override
	public LuceneIndexFieldType<BigInteger> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		int resolvedDecimalScale = resolveDecimalScale();
		if ( resolvedDecimalScale > 0 ) {
			throw log.invalidDecimalScale( resolvedDecimalScale, getBuildContext().getEventContext() );
		}

		ToDocumentFieldValueConverter<?, ? extends BigInteger> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super BigInteger, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneBigIntegerFieldCodec codec = new LuceneBigIntegerFieldCodec( resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue, resolvedDecimalScale );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec )
		);
	}

	@Override
	protected LuceneBigIntegerIndexFieldTypeContext thisAsS() {
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
