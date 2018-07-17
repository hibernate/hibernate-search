/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.types.formatter.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortContributor;
import org.hibernate.search.backend.lucene.types.sort.impl.SortMissingValue;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

class FieldSortBuilderImpl extends AbstractSearchSortBuilder
		implements FieldSortBuilder<LuceneSearchSortBuilder> {

	private final String absoluteFieldPath;

	private final LuceneFieldFormatter<?> fieldFormatter;

	private final LuceneFieldSortContributor fieldSortContributor;

	private Object missingValue;

	FieldSortBuilderImpl(String absoluteFieldPath, LuceneFieldFormatter<?> fieldFormatter, LuceneFieldSortContributor fieldSortContributor) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.fieldFormatter = fieldFormatter;
		this.fieldSortContributor = fieldSortContributor;
	}

	@Override
	public void missingFirst() {
		missingValue = SortMissingValue.MISSING_FIRST;
	}

	@Override
	public void missingLast() {
		missingValue = SortMissingValue.MISSING_LAST;
	}

	@Override
	public void missingAs(Object value) {
		missingValue = fieldFormatter.format( value );
	}

	@Override
	public void buildAndAddTo(LuceneSearchSortCollector collector) {
		fieldSortContributor.contribute( collector, absoluteFieldPath, order, missingValue );
	}
}
