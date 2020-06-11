/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneTemporalFieldSortBuilderFactory;

abstract class AbstractLuceneTemporalIndexFieldTypeOptionsStep<
				S extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<S, F>,
				F extends TemporalAccessor
		>
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<S, F> {

	AbstractLuceneTemporalIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType) {
		super( buildContext, fieldType );
	}

	@Override
	protected LuceneNumericFieldSortBuilderFactory<F, ?> createFieldSortBuilderFactory(boolean resolvedSortable,
			AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new LuceneTemporalFieldSortBuilderFactory<>( resolvedSortable, codec );
	}
}
