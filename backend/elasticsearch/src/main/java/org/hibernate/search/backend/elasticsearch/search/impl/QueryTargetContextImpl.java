/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactory;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.QueryTargetContext;

class QueryTargetContextImpl implements QueryTargetContext {

	private final ElasticsearchClauseFactory clauseFactory;

	private final Set<ElasticsearchIndexModel> indexModels;

	private final Set<String> indexNames;

	public QueryTargetContextImpl(Set<ElasticsearchIndexModel> indexModels) {
		this.clauseFactory = new ElasticsearchClauseFactoryImpl( indexModels );
		this.indexModels = indexModels;
		this.indexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getIndexName )
				.collect( Collectors.toSet() );
	}

	@Override
	public ElasticsearchClauseFactory getClauseFactory() {
		return clauseFactory;
	}

	public Set<ElasticsearchIndexModel> getIndexModels() {
		return indexModels;
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}
}
