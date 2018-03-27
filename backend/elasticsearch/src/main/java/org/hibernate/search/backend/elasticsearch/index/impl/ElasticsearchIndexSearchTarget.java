/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchTargetContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBase;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

class ElasticsearchIndexSearchTarget extends IndexSearchTargetBase {

	private final ElasticsearchSearchTargetModel searchTargetModel;
	private final SearchTargetContext<?> searchTargetContext;

	ElasticsearchIndexSearchTarget(ElasticsearchBackend backend, Set<ElasticsearchIndexModel> indexModels) {
		this.searchTargetModel = new ElasticsearchSearchTargetModel( indexModels );
		this.searchTargetContext = new ElasticsearchSearchTargetContext( backend, searchTargetModel );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchTargetModel.getIndexNames() )
				.append( "]")
				.toString();
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			SessionContext context,
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
