/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFilterNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.engine.search.predicate.spi.FilterPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneFilterPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
	implements FilterPredicateBuilder<LuceneSearchPredicateBuilder> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchPredicateBuilderFactory predicateBuilderFactory;
	private final LuceneSearchContext searchContext;

	private final String name;
	private final Map<String, Object> params = new LinkedHashMap<>();
	private final LuceneIndexSchemaFilterNode<?> filterNode;

	LuceneFilterPredicateBuilder(LuceneSearchContext searchContext,
		LuceneSearchPredicateBuilderFactory predicateBuilderFactory,
		String name, LuceneIndexSchemaFilterNode<?> filter) {
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.name = name;
		this.searchContext = searchContext;
		this.filterNode = filter;
	}

	@Override
	public void param(String name, Object value) {
		params.put( name, value );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {

		Map<String, Object> computedParams = new LinkedHashMap<>();
		computedParams.putAll( params );
		computedParams.putAll( filterNode.getParams() );

		SearchPredicateFactory factory = new DefaultSearchPredicateFactory<>( predicateBuilderFactory );
		LuceneFilterFactoryContextImpl ctx = new LuceneFilterFactoryContextImpl(
			factory, filterNode.getParentDocumentPath(), filterNode.getNestedDocumentPath(), filterNode.getAbsoluteFilterPath(), computedParams );

		Query luceneFilter = null;

		FilterFactory filterFactory = filterNode.getFactory();

		SearchPredicate searchPredicate = filterFactory.create( ctx );
		searchPredicate = (SearchPredicate) predicateBuilderFactory.toImplementation( searchPredicate );

		if ( searchPredicate instanceof LuceneSearchPredicateBuilder ) {
			LuceneSearchPredicateContext filterContext = new LuceneSearchPredicateContext( ctx.getNestedPath() );
			luceneFilter = ((LuceneSearchPredicateBuilder) searchPredicate).build( filterContext );
		}
		else {
			throw log.unableToCreateFilterForSearch( name );
		}

		return luceneFilter;
	}
}
