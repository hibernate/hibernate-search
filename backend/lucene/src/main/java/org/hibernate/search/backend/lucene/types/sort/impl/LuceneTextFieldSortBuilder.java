/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneTextFieldComparatorSource;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.SortField;

public class LuceneTextFieldSortBuilder<F>
		extends AbstractLuceneStandardFieldSortBuilder<F, String, LuceneTextFieldCodec<F>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	LuceneTextFieldSortBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
			LuceneTextFieldCodec<F> codec) {
		super( searchContext, field, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
	}

	@Override
	protected Object encodeMissingAs(F converted) {
		return codec.normalize( absoluteFieldPath, codec.encode( converted ) );
	}

	@Override
	public void mode(SortMode mode) {
		switch ( mode ) {
			case MIN:
			case MAX:
				super.mode( mode );
				break;
			case SUM:
			case AVG:
			case MEDIAN:
			default:
				throw log.cannotComputeSumOrAvgOrMedianForStringField( getEventContext() );
		}
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		LuceneTextFieldComparatorSource fieldComparatorSource = new LuceneTextFieldComparatorSource(
				nestedDocumentPath, missingValue, getMultiValueMode(), getNestedFilter()
		);
		SortField sortField = new SortField( absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );

		collector.collectSortField( sortField, ( nestedDocumentPath != null ) ? fieldComparatorSource : null );
	}
}
