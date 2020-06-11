/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class LuceneTemporalFieldSortBuilderFactory<F extends TemporalAccessor, E extends Number>
		extends LuceneNumericFieldSortBuilderFactory<F, E> {

	public LuceneTemporalFieldSortBuilderFactory(boolean sortable, AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( sortable, codec );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSortable( field );
		return new LuceneTemporalFieldSortBuilder<>(
				searchContext, field, codec
		);
	}
}
