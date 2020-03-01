/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneNumericFieldComparatorSource;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

public class LuceneNumericFieldSortBuilder<F, E extends Number>
	extends AbstractLuceneStandardFieldSortBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericFieldSortBuilder(LuceneSearchContext searchContext,
		String absoluteFieldPath, String nestedDocumentPath,
		DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
		LuceneCompatibilityChecker converterChecker, AbstractLuceneNumericFieldCodec<F, E> codec) {
		super(
			searchContext, absoluteFieldPath, nestedDocumentPath,
			converter, rawConverter, converterChecker, codec,
			codec.getDomain().getMinValue(),
			codec.getDomain().getMaxValue()
		);
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		LuceneNumericFieldComparatorSource<E> fieldComparatorSource = new LuceneNumericFieldComparatorSource<>(
			nestedDocumentPath, codec.getDomain(), (E) getEffectiveMissingValue( missingValue, order ), getMultiValueMode() );
		SortField sortField = new SortField( absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );

		collector.collectSortField( sortField, (nestedDocumentPath != null) ? fieldComparatorSource : null );
	}

	protected MultiValueMode getMultiValueMode() {
		MultiValueMode sortMode = MultiValueMode.MIN;
		if ( multi != null ) {
			switch ( multi ) {
				case MIN:
					sortMode = MultiValueMode.MIN;
					break;
				case MAX:
					sortMode = MultiValueMode.MAX;
					break;
				case AVG:
					sortMode = MultiValueMode.AVG;
					break;
				case SUM:
					sortMode = MultiValueMode.SUM;
					break;
				case MEDIAN:
					sortMode = MultiValueMode.MEDIAN;
					break;
			}
		}
		return sortMode;
	}

}
