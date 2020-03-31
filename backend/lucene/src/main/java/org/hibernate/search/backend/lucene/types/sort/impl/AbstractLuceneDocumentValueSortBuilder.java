/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;
import org.apache.lucene.search.Query;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractLuceneSearchSortBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractLuceneDocumentValueSortBuilder
		extends AbstractLuceneSearchSortBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String absoluteFieldPath;
	protected final String nestedDocumentPath;
	private SortMode mode;

	protected AbstractLuceneDocumentValueSortBuilder(String absoluteFieldPath, String nestedDocumentPath) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
	}

	public void mode(SortMode mode) {
		if ( nestedDocumentPath != null && SortMode.MEDIAN.equals( mode ) ) {
			throw log.cannotComputeMedianAcrossNested( getEventContext() );
		}
		this.mode = mode;
	}

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

	protected Query getLuceneFilter() {
		if ( filter == null ) {
			return null;
		}

		Query luceneFilter = null;
		if ( filter instanceof LuceneSearchPredicateBuilder ) {
			LuceneSearchPredicateContext filterContext = new LuceneSearchPredicateContext( absoluteFieldPath );
			luceneFilter = ((LuceneSearchPredicateBuilder) filter).build( filterContext );
		}
		else {
			throw log.unableToCreateNestedSortFilter( absoluteFieldPath );
		}

		return luceneFilter;
	}

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
	}

}
