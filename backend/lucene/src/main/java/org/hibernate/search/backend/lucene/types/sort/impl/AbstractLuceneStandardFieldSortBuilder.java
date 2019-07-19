/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractLuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <E> The encoded type.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
abstract class AbstractLuceneStandardFieldSortBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
		extends AbstractLuceneSearchSortBuilder
		implements FieldSortBuilder<LuceneSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;

	protected final String absoluteFieldPath;

	protected final ToDocumentFieldValueConverter<?, ? extends F> converter;
	private final ToDocumentFieldValueConverter<F, ? extends F> rawConverter;
	private final LuceneCompatibilityChecker converterChecker;

	protected final C codec;
	private final Object sortMissingValueFirstPlaceholder;
	private final Object sortMissingValueLastPlaceholder;

	protected Object missingValue;

	protected AbstractLuceneStandardFieldSortBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, C codec,
			Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.converterChecker = converterChecker;
		this.codec = codec;
		this.sortMissingValueFirstPlaceholder = sortMissingValueFirstPlaceholder;
		this.sortMissingValueLastPlaceholder = sortMissingValueLastPlaceholder;
	}

	@Override
	public void missingFirst() {
		missingValue = SortMissingValue.MISSING_FIRST;
	}

	@Override
	public void missingLast() {
		missingValue = SortMissingValue.MISSING_LAST;
	}

	@Override
	public void missingAs(Object value, ValueConvert convert) {
		ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter = getDslToIndexConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			missingValue = encodeMissingAs( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	protected Object encodeMissingAs(F converted) {
		return codec.encode( converted );
	}

	protected void setEffectiveMissingValue(SortField sortField, Object missingValue, SortOrder order) {
		if ( missingValue == null ) {
			return;
		}

		Object effectiveMissingValue;
		if ( missingValue == SortMissingValue.MISSING_FIRST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueLastPlaceholder : sortMissingValueFirstPlaceholder;
		}
		else if ( missingValue == SortMissingValue.MISSING_LAST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueFirstPlaceholder : sortMissingValueLastPlaceholder;
		}
		else {
			effectiveMissingValue = missingValue;
		}

		sortField.setMissingValue( effectiveMissingValue );
	}

	private ToDocumentFieldValueConverter<?, ? extends F> getDslToIndexConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				converterChecker.failIfNotCompatible();
				return converter;
		}
	}
}
