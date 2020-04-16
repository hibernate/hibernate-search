/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneNestableAggregation<A> implements LuceneSearchAggregation<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String nestedDocumentPath;
	private final Query nestedFilter;

	AbstractLuceneNestableAggregation(AbstractBuilder<A> builder) {
		this.nestedDocumentPath = builder.nestedDocumentPath;
		this.nestedFilter = builder.getLuceneFilter();
	}

	protected NestedDocsProvider createNestedDocsProvider(AggregationExtractContext context) {
		NestedDocsProvider nestedDocsProvider = null;
		if ( nestedDocumentPath != null ) {
			nestedDocsProvider = context.createNestedDocsProvider( nestedDocumentPath, nestedFilter );
		}
		return nestedDocsProvider;
	}

	public abstract static class AbstractBuilder<A> implements SearchAggregationBuilder<A> {

		private final String nestedDocumentPath;
		private SearchPredicate filter;

		public AbstractBuilder(String nestedDocumentPath) {
			this.nestedDocumentPath = nestedDocumentPath;
		}

		public void filter(SearchPredicate filter) {
			this.filter = filter;
		}

		@Override
		public abstract LuceneSearchAggregation<A> build();

		private Query getLuceneFilter() {
			if ( filter == null ) {
				return null;
			}

			Query luceneFilter;
			if ( filter instanceof LuceneSearchPredicateBuilder ) {
				LuceneSearchPredicateContext filterContext = new LuceneSearchPredicateContext( nestedDocumentPath );
				luceneFilter = ((LuceneSearchPredicateBuilder) filter).build( filterContext );
			}
			else {
				throw log.unableToCreateNestedAggregationFilter( nestedDocumentPath );
			}

			return luceneFilter;
		}

	}
}
