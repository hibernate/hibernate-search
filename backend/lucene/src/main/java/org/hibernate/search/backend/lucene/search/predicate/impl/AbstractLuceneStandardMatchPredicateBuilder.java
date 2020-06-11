/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <E> The encoded type.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
public abstract class AbstractLuceneStandardMatchPredicateBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
		extends AbstractLuceneSingleFieldPredicateBuilder
		implements MatchPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	protected final LuceneSearchFieldContext<F> field;
	protected final C codec;

	protected E value;

	protected AbstractLuceneStandardMatchPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, C codec) {
		super( field );
		this.searchContext = searchContext;
		this.field = field;
		this.codec = codec;
	}

	@Override
	public void fuzzy(int maxEditDistance, int exactPrefixLength) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public void analyzer(String analyzerName) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public void skipAnalysis() {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public void value(Object value, ValueConvert convert) {
		DslConverter<?, ? extends F> dslToIndexConverter = field.type().dslConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.toDocumentFieldValueConvertContext() );
			this.value = codec.encode( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
		}
	}
}
