/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;

public abstract class AbstractLuceneCodecAwareSearchFieldQueryElementFactory<T, F, C extends LuceneFieldCodec<F>>
		extends AbstractLuceneSearchFieldQueryElementFactory<T, F> {

	protected final C codec;

	protected AbstractLuceneCodecAwareSearchFieldQueryElementFactory(C codec) {
		this.codec = codec;
	}

	@Override
	public boolean isCompatibleWith(LuceneSearchFieldQueryElementFactory<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneCodecAwareSearchFieldQueryElementFactory<?, ?, ?> castedOther =
				(AbstractLuceneCodecAwareSearchFieldQueryElementFactory<?, ?, ?>) other;
		return codec.isCompatibleWith( castedOther.codec );
	}
}
