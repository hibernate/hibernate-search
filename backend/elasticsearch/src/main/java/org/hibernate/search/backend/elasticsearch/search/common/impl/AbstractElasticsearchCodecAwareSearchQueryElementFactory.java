/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchCodecAwareSearchQueryElementFactory<T, F>
		extends AbstractElasticsearchValueFieldSearchQueryElementFactory<T, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchFieldCodec<F> codec;

	protected AbstractElasticsearchCodecAwareSearchQueryElementFactory(ElasticsearchFieldCodec<F> codec) {
		this.codec = codec;
	}

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		super.checkCompatibleWith( other );
		AbstractElasticsearchCodecAwareSearchQueryElementFactory<?, ?> castedOther =
				(AbstractElasticsearchCodecAwareSearchQueryElementFactory<?, ?>) other;
		if ( !codec.isCompatibleWith( castedOther.codec ) ) {
			throw log.differentFieldCodecForQueryElement( codec, castedOther.codec );
		}
	}
}
