/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractLuceneReversibleSort;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
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

	protected final SortField sortField;

	protected AbstractLuceneDocumentValueSort(AbstractBuilder builder) {
		super( builder );
		LuceneFieldComparatorSource fieldComparatorSource = builder.toFieldComparatorSource();
		sortField = new SortField( builder.absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortField( sortField );
	}

	public abstract static class AbstractBuilder extends AbstractLuceneReversibleSort.AbstractBuilder {
		protected final String absoluteFieldPath;
		protected final String nestedDocumentPath;
		private SortMode mode;
		protected Query nestedFilter;

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
			PredicateRequestContext filterContext = new PredicateRequestContext( nestedDocumentPath );
			this.nestedFilter = luceneFilter.toQuery( filterContext );
		}

		protected abstract LuceneFieldComparatorSource toFieldComparatorSource();

		protected final MultiValueMode getMultiValueMode() {
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

		protected Query getNestedFilter() {
			return nestedFilter;
		}

		protected final EventContext getEventContext() {
			return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
		}
	}
}
