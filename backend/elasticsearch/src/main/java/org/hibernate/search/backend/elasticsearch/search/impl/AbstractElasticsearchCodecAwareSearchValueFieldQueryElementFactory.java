/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory<T, F>
		extends AbstractElasticsearchSearchValueFieldQueryElementFactory<T, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchFieldCodec<F> codec;

	protected AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory(ElasticsearchFieldCodec<F> codec) {
		this.codec = codec;
	}

	@Override
	public void checkCompatibleWith(ElasticsearchSearchValueFieldQueryElementFactory<?, ?> other) {
		super.checkCompatibleWith( other );
		AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory<?, ?> castedOther =
				(AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory<?, ?>) other;
		if ( !codec.isCompatibleWith( castedOther.codec ) ) {
			throw log.differentFieldCodecForQueryElement( codec, castedOther.codec );
		}
	}
}
