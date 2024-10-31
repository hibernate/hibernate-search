/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public abstract class AbstractElasticsearchCodecAwareSearchQueryElementFactory<T, F>
		extends AbstractElasticsearchValueFieldSearchQueryElementFactory<T, F> {

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
			throw QueryLog.INSTANCE.differentFieldCodecForQueryElement( codec, castedOther.codec );
		}
	}
}
