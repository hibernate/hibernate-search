/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;

public abstract class AbstractElasticsearchSearchFieldQueryElementFactory<T, F>
		implements ElasticsearchSearchFieldQueryElementFactory<T, F> {

	protected final ElasticsearchFieldCodec<F> codec;

	public AbstractElasticsearchSearchFieldQueryElementFactory(ElasticsearchFieldCodec<F> codec) {
		this.codec = codec;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchSearchFieldQueryElementFactory<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractElasticsearchSearchFieldQueryElementFactory<?, ?> castedOther =
				(AbstractElasticsearchSearchFieldQueryElementFactory<?, ?>) other;
		return codec.isCompatibleWith( castedOther.codec );
	}
}
