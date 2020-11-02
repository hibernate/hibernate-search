/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.AbstractElasticsearchReversibleSort;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class AbstractElasticsearchDocumentValueSort extends AbstractElasticsearchReversibleSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MODE_ACCESSOR = JsonAccessor.root().property( "mode" );
	private static final JsonPrimitive SUM_KEYWORD_JSON = new JsonPrimitive( "sum" );
	private static final JsonPrimitive AVG_KEYWORD_JSON = new JsonPrimitive( "avg" );
	private static final JsonPrimitive MIN_KEYWORD_JSON = new JsonPrimitive( "min" );
	private static final JsonPrimitive MAX_KEYWORD_JSON = new JsonPrimitive( "max" );
	private static final JsonPrimitive MEDIAN_KEYWORD_JSON = new JsonPrimitive( "median" );

	protected final String absoluteFieldPath;
	protected final List<String> nestedPathHierarchy;
	private final ElasticsearchSearchSyntax searchSyntax;

	private final JsonPrimitive mode;
	private final ElasticsearchSearchPredicate filter;

	AbstractElasticsearchDocumentValueSort(AbstractBuilder<?> builder) {
		super( builder );
		absoluteFieldPath = builder.field.absolutePath();
		nestedPathHierarchy = builder.nestedPathHierarchy;
		searchSyntax = builder.searchSyntax;
		mode = builder.mode;
		filter = builder.filter;
	}

	@Override
	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( !nestedPathHierarchy.isEmpty() ) {
			JsonObject jsonFilter = getJsonFilter( collector.getRootPredicateContext() );
			searchSyntax.requestNestedSort( nestedPathHierarchy, innerObject, jsonFilter );
		}

		if ( mode != null ) {
			MODE_ACCESSOR.set( innerObject, mode );
		}
	}

	private JsonObject getJsonFilter(PredicateRequestContext rootPredicateContext) {
		if ( filter == null ) {
			return null;
		}
		String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
		PredicateRequestContext filterContext = rootPredicateContext.withNestedPath( lastNestedPath );
		return filter.toJsonQuery( filterContext );
	}

	abstract static class AbstractBuilder<F> extends AbstractElasticsearchReversibleSort.AbstractBuilder {
		private final ElasticsearchSearchSyntax searchSyntax;
		protected final ElasticsearchSearchValueFieldContext<F> field;
		protected final List<String> nestedPathHierarchy;

		private JsonPrimitive mode;
		private ElasticsearchSearchPredicate filter;

		AbstractBuilder(ElasticsearchSearchContext searchContext, ElasticsearchSearchValueFieldContext<F> field) {
			super( searchContext );
			this.searchSyntax = searchContext.searchSyntax();
			this.field = field;
			this.nestedPathHierarchy = field.nestedPathHierarchy();
		}

		public void mode(SortMode mode) {
			if ( !nestedPathHierarchy.isEmpty() && SortMode.MEDIAN.equals( mode ) ) {
				throw log.invalidSortModeAcrossNested( mode, field.eventContext() );
			}
			if ( mode != null ) {
				switch ( mode ) {
					case SUM:
						this.mode = SUM_KEYWORD_JSON;
						break;
					case AVG:
						this.mode = AVG_KEYWORD_JSON;
						break;
					case MIN:
						this.mode = MIN_KEYWORD_JSON;
						break;
					case MAX:
						this.mode = MAX_KEYWORD_JSON;
						break;
					case MEDIAN:
						this.mode = MEDIAN_KEYWORD_JSON;
						break;
					default:
						throw new AssertionFailure( "Unexpected sort mode: " + mode );
				}
			}
		}

		public void filter(SearchPredicate filter) {
			if ( nestedPathHierarchy.isEmpty() ) {
				throw log.cannotFilterSortOnRootDocumentField( field.absolutePath(), field.eventContext() );
			}
			ElasticsearchSearchPredicate elasticsearchFilter = ElasticsearchSearchPredicate.from( searchContext, filter );
			elasticsearchFilter.checkNestableWithin( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
			this.filter = elasticsearchFilter;
		}
	}
}
