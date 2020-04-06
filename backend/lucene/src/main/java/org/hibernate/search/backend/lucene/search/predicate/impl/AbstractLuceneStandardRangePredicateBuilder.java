/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <E> The encoded type.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
public abstract class AbstractLuceneStandardRangePredicateBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
		extends AbstractLuceneSingleFieldPredicateBuilder
		implements RangePredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;

	private final DslConverter<?, ? extends F> converter;
	private final DslConverter<F, ? extends F> rawConverter;
	private final LuceneCompatibilityChecker converterChecker;

	protected final C codec;

	protected Range<E> range;

	protected AbstractLuceneStandardRangePredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, C codec) {
		super( absoluteFieldPath, nestedPathHierarchy );
		this.searchContext = searchContext;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.converterChecker = converterChecker;
		this.codec = codec;
	}

	@Override
	public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
		this.range = Range.between(
				convertAndEncode( range.getLowerBoundValue(), convertLowerBound ),
				range.getLowerBoundInclusion(),
				convertAndEncode( range.getUpperBoundValue(), convertUpperBound ),
				range.getUpperBoundInclusion()
		);
	}

	private E convertAndEncode(Optional<?> valueOptional, ValueConvert convert) {
		if ( !valueOptional.isPresent() ) {
			return null;
		}
		Object value = valueOptional.get();
		DslConverter<?, ? extends F> toFieldValueConverter = getDslToIndexConverter( convert );
		try {
			F converted = toFieldValueConverter.convertUnknown(
					value, searchContext.getToDocumentFieldValueConvertContext()
			);
			return codec.encode( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	private DslConverter<?, ? extends F> getDslToIndexConverter(ValueConvert convert) {
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
