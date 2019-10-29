/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
abstract class AbstractLuceneStandardFieldPredicateBuilderFactory<F, C extends LuceneStandardFieldCodec<F, ?>>
		extends AbstractLuceneFieldPredicateBuilderFactory {

	protected final DslConverter<?, ? extends F> converter;
	protected final DslConverter<F, ? extends F> rawConverter;

	final C codec;

	AbstractLuceneStandardFieldPredicateBuilderFactory( boolean searchable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			C codec) {
		super( searchable );
		Contracts.assertNotNull( converter, "converter" );
		Contracts.assertNotNull( rawConverter, "rawConverter" );
		Contracts.assertNotNull( codec, "codec" );
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public C getCodec() {
		return codec;
	}

	@Override
	public DslConverter<?, ? extends F> getConverter() {
		return converter;
	}
}
