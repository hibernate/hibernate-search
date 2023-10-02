/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractLuceneReversibleSort;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.search.sort.impl.SortRequestContext;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public abstract class AbstractLuceneDocumentValueSort extends AbstractLuceneReversibleSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;
	private final LuceneSearchPredicate nestedFilter;
	private final String nestedDocumentPath;
	private final MultiValueMode multiValueMode;

	protected AbstractLuceneDocumentValueSort(AbstractBuilder builder) {
		super( builder );
		absoluteFieldPath = builder.absoluteFieldPath;
		nestedFilter = builder.nestedFilter;
		nestedDocumentPath = builder.nestedDocumentPath;
		multiValueMode = builder.getMultiValueMode();
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		var fieldComparatorSource = createFieldComparatorSource( collector );
		var sortField = new SortField( this.absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );
		collector.collectSortField( sortField );
	}

	private LuceneFieldComparatorSource createFieldComparatorSource(LuceneSearchSortCollector collector) {
		Query nestedFilter = getNestedFilter( collector );
		return doCreateFieldComparatorSource( nestedDocumentPath, multiValueMode, nestedFilter );
	}

	protected abstract LuceneFieldComparatorSource doCreateFieldComparatorSource(String nestedDocumentPath,
			MultiValueMode multiValueMode, Query nestedFilter);

	private Query getNestedFilter(SortRequestContext context) {
		return nestedFilter == null
				? null
				: nestedFilter.toQuery( context.toPredicateRequestContext( nestedDocumentPath ) );
	}

	public abstract static class AbstractBuilder extends AbstractLuceneReversibleSort.AbstractBuilder {
		protected final String absoluteFieldPath;
		protected final String nestedDocumentPath;
		private SortMode mode;
		protected LuceneSearchPredicate nestedFilter;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<?> field) {
			this( scope, field.absolutePath(), field.nestedDocumentPath() );
		}

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope,
				String absoluteFieldPath, String nestedDocumentPath) {
			super( scope );
			this.absoluteFieldPath = absoluteFieldPath;
			this.nestedDocumentPath = nestedDocumentPath;
		}

		public void mode(SortMode mode) {
			if ( nestedDocumentPath != null && SortMode.MEDIAN.equals( mode ) ) {
				throw log.invalidSortModeAcrossNested( mode, getEventContext() );
			}
			this.mode = mode;
		}

		public void filter(SearchPredicate filter) {
			if ( nestedDocumentPath == null ) {
				throw log.cannotFilterSortOnRootDocumentField( absoluteFieldPath, getEventContext() );
			}
			LuceneSearchPredicate luceneFilter = LuceneSearchPredicate.from( scope, filter );
			luceneFilter.checkNestableWithin( nestedDocumentPath );
			this.nestedFilter = luceneFilter;
		}

		private MultiValueMode getMultiValueMode() {
			MultiValueMode multiValueMode;
			if ( mode == null ) {
				multiValueMode = order == SortOrder.DESC ? MultiValueMode.MAX : MultiValueMode.MIN;
			}
			else {
				switch ( mode ) {
					case MIN:
						multiValueMode = MultiValueMode.MIN;
						break;
					case MAX:
						multiValueMode = MultiValueMode.MAX;
						break;
					case AVG:
						multiValueMode = MultiValueMode.AVG;
						break;
					case SUM:
						multiValueMode = MultiValueMode.SUM;
						break;
					case MEDIAN:
						multiValueMode = MultiValueMode.MEDIAN;
						break;
					default:
						throw new AssertionFailure( "Unexpected sort mode: " + mode );
				}
			}
			return multiValueMode;
		}

		protected final EventContext getEventContext() {
			return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
		}
	}
}
