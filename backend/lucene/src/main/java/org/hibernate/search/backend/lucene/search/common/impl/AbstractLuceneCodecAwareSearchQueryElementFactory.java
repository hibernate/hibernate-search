/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneCodecAwareSearchQueryElementFactory<T, F, C extends LuceneFieldCodec<F>>
		extends AbstractLuceneValueFieldSearchQueryElementFactory<T, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final C codec;

	protected AbstractLuceneCodecAwareSearchQueryElementFactory(C codec) {
		this.codec = codec;
	}

	@Override
	public void checkCompatibleWith(LuceneSearchQueryElementFactory<?, ?> other) {
		super.checkCompatibleWith( other );
		AbstractLuceneCodecAwareSearchQueryElementFactory<?, ?, ?> castedOther =
				(AbstractLuceneCodecAwareSearchQueryElementFactory<?, ?, ?>) other;
		if ( !codec.isCompatibleWith( castedOther.codec ) ) {
			throw log.differentFieldCodecForQueryElement( codec, castedOther.codec );
		}
	}
}
