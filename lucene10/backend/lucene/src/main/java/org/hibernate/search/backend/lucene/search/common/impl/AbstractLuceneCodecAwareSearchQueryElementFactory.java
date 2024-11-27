/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public abstract class AbstractLuceneCodecAwareSearchQueryElementFactory<T, F, C extends LuceneFieldCodec<F, ?>>
		extends AbstractLuceneValueFieldSearchQueryElementFactory<T, F> {

	protected final C codec;

	protected AbstractLuceneCodecAwareSearchQueryElementFactory(C codec) {
		this.codec = codec;
	}

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		super.checkCompatibleWith( other );
		AbstractLuceneCodecAwareSearchQueryElementFactory<?, ?, ?> castedOther =
				(AbstractLuceneCodecAwareSearchQueryElementFactory<?, ?, ?>) other;
		if ( !codec.isCompatibleWith( castedOther.codec ) ) {
			throw QueryLog.INSTANCE.differentFieldCodecForQueryElement( codec, castedOther.codec );
		}
	}
}
