/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.SortField;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

public class LuceneLocalDateFieldSortBuilder extends AbstractLuceneFieldSortBuilder {

	LuceneLocalDateFieldSortBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, LuceneFieldConverter<?, Long> converter) {
		super( searchContext, absoluteFieldPath, converter, Long.MIN_VALUE, Long.MAX_VALUE );
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		SortField sortField = new SortField( absoluteFieldPath, SortField.Type.LONG, order == SortOrder.DESC );
		setEffectiveMissingValue( sortField, missingValue, order );

		collector.collectSortField( sortField );
	}
}
