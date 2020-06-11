/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
abstract class AbstractLuceneStandardFieldPredicateBuilderFactory<F, C extends LuceneStandardFieldCodec<F, ?>>
		extends AbstractLuceneFieldPredicateBuilderFactory<F> {

	final C codec;

	AbstractLuceneStandardFieldPredicateBuilderFactory(boolean searchable, C codec) {
		super( searchable );
		Contracts.assertNotNull( codec, "codec" );
		this.codec = codec;
	}

	@Override
	public C getCodec() {
		return codec;
	}

}
