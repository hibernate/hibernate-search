/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class LuceneTemporalFieldSortBuilderFactory<F extends TemporalAccessor, E extends Number>
		extends LuceneNumericFieldSortBuilderFactory<F, E> {

	public LuceneTemporalFieldSortBuilderFactory(boolean sortable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( sortable, converter, rawConverter, codec );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, String nestedDocumentPath, LuceneCompatibilityChecker converterChecker) {
		checkSortable( absoluteFieldPath );

		return new LuceneTemporalFieldSortBuilder<>(
				searchContext, absoluteFieldPath, nestedDocumentPath, converter, rawConverter, converterChecker, codec
		);
	}
}
