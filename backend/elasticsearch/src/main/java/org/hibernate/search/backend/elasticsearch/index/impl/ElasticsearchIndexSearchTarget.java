/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchTargetContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBase;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

class ElasticsearchIndexSearchTarget extends IndexSearchTargetBase {

	private final ElasticsearchSearchTargetModel searchTargetModel;
	private final SearchTargetContext<?> searchTargetContext;

	ElasticsearchIndexSearchTarget(SearchBackendContext searchBackendContext,
			MappingContextImplementor mappingContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
		this.searchTargetContext = new ElasticsearchSearchTargetContext( mappingContext, searchBackendContext, searchTargetModel );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchTargetModel.getHibernateSearchIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			SessionContextImplementor context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		return new SearchQueryResultDefinitionContextImpl<>( searchTargetContext, context,
				documentReferenceTransformer, objectLoader );
	}

	@Override
	protected SearchTargetContext<?> getSearchTargetContext() {
		return searchTargetContext;
	}
}
