/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import com.google.gson.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFilterNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.engine.search.predicate.spi.FilterPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchFilterPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
	implements FilterPredicateBuilder<ElasticsearchSearchPredicateBuilder> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchPredicateBuilderFactory predicateBuilderFactory;
	private final ElasticsearchSearchContext searchContext;

	private final String name;
	private final Map<String, Object> params = new LinkedHashMap<>();
	private final ElasticsearchIndexSchemaFilterNode<?> filterNode;

	ElasticsearchFilterPredicateBuilder(ElasticsearchSearchContext searchContext,
		ElasticsearchSearchPredicateBuilderFactory predicateBuilderFactory,
		String name, ElasticsearchIndexSchemaFilterNode<?> filterNode) {
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.name = name;
		this.searchContext = searchContext;
		this.filterNode = filterNode;
	}

	@Override
	public void param(String name, Object value) {
		params.put( name, value );
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context, JsonObject outerObject, JsonObject innerObject) {

		Map<String, Object> computedParams = new LinkedHashMap<>();
		computedParams.putAll( params );
		computedParams.putAll( filterNode.getParams() );

		SearchPredicateFactory factory = new DefaultSearchPredicateFactory<>( predicateBuilderFactory );
		ElasticsearchFilterFactoryContextImpl ctx = new ElasticsearchFilterFactoryContextImpl(
			factory, filterNode.getParentDocumentPath(), filterNode.getNestedDocumentPath(), filterNode.getAbsoluteFilterPath(), computedParams );

		JsonObject jsonFilter = null;

		FilterFactory filterFactory = filterNode.getFactory();

		SearchPredicate filter = filterFactory.create( ctx );
		filter = (SearchPredicate) predicateBuilderFactory.toImplementation( filter );

		if ( filter instanceof ElasticsearchSearchPredicateBuilder ) {
			jsonFilter = ((ElasticsearchSearchPredicateBuilder) filter).build( context );
		}
		else {
			throw log.unableToCreateFilterForSearch( name );
		}

		return jsonFilter;
	}
}
