/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneTemporalFieldSortBuilder<F extends TemporalAccessor, E extends Number>
		extends LuceneNumericFieldSortBuilder<F, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	LuceneTemporalFieldSortBuilder(LuceneSearchContext searchContext, String absoluteFieldPath,
			String nestedDocumentPath,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, absoluteFieldPath, nestedDocumentPath, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public void mode(SortMode mode) {
		switch ( mode ) {
			case MIN:
			case MAX:
			case AVG:
			case MEDIAN:
				super.mode( mode );
				break;
			case SUM:
				throw log.cannotComputeSumForTemporalField( getEventContext() );
		}
	}
}
