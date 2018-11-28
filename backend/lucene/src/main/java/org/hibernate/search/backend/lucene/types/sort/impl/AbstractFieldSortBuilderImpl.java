/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractFieldSortBuilderImpl extends AbstractSearchSortBuilder
		implements FieldSortBuilder<LuceneSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;

	protected final String absoluteFieldPath;

	protected final LuceneFieldConverter<?, ?> converter;

	protected Object missingValue;

	private Object sortMissingValueFirstPlaceholder;

	private Object sortMissingValueLastPlaceholder;

	protected AbstractFieldSortBuilderImpl(
			LuceneSearchContext searchContext,
			String absoluteFieldPath, LuceneFieldConverter<?, ?> converter,
			Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.sortMissingValueFirstPlaceholder = sortMissingValueFirstPlaceholder;
		this.sortMissingValueLastPlaceholder = sortMissingValueLastPlaceholder;
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
		try {
			missingValue = converter.convertDslToIndex( value, searchContext.getToIndexFieldValueConvertContext() );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	protected void setEffectiveMissingValue(SortField sortField, Object missingValue, SortOrder order) {
		if ( missingValue == null ) {
			return;
		}

		// TODO so this is to mimic the Elasticsearch behavior, I'm not totally convinced it's the good choice though
		Object effectiveMissingValue;
		if ( missingValue == SortMissingValue.MISSING_FIRST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueLastPlaceholder : sortMissingValueFirstPlaceholder;
		}
		else if ( missingValue == SortMissingValue.MISSING_LAST ) {
			effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueFirstPlaceholder : sortMissingValueLastPlaceholder;
		}
		else {
			effectiveMissingValue = missingValue;
		}

		sortField.setMissingValue( effectiveMissingValue );
	}
}
