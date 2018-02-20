/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

class FieldSortBuilderImpl extends AbstractSearchSortBuilder
		implements FieldSortBuilder<LuceneSearchSortCollector> {

	private final String absoluteFieldPath;

	private final LuceneFieldFormatter<?> fieldFormatter;

	private Object missingValue;

	FieldSortBuilderImpl(String absoluteFieldPath, LuceneFieldFormatter<?> fieldFormatter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.fieldFormatter = fieldFormatter;
	}

	@Override
	public void missingFirst() {
		missingValue = Missing.FIRST;
	}

	@Override
	public void missingLast() {
		missingValue = Missing.LAST;
	}

	@Override
	public void missingAs(Object value) {
		missingValue = fieldFormatter.format( value );
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector) {
		// X so this is to mimic the Elasticsearch behavior, I'm not totally convinced it's the good choice though
		if ( missingValue == Missing.FIRST ) {
			missingValue = order == SortOrder.DESC ? fieldFormatter.getSortMissingLast() : fieldFormatter.getSortMissingFirst();
		}
		else if ( missingValue == Missing.LAST ) {
			missingValue = order == SortOrder.DESC ? fieldFormatter.getSortMissingFirst() : fieldFormatter.getSortMissingLast();
		}

		SortField sortField = new SortField( absoluteFieldPath, fieldFormatter.getDefaultSortFieldType(), order == SortOrder.DESC ? true : false );
		if ( missingValue != null ) {
			sortField.setMissingValue( missingValue );
		}

		collector.collectSortField( sortField );
	}

	private enum Missing {
		FIRST,
		LAST;
	}
}
