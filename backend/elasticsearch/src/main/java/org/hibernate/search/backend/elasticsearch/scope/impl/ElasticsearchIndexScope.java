/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;

public class ElasticsearchIndexScope
		implements IndexScope {

	private final ElasticsearchSearchQueryIndexScope<?> searchScope;

	public ElasticsearchIndexScope(BackendMappingContext mappingContext, SearchBackendContext backendContext,
			Set<ElasticsearchIndexModel> indexModels) {
		this.searchScope = backendContext.createSearchContext( mappingContext, indexModels );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexNames=" + searchScope.hibernateSearchIndexNames() + "]";
	}

	@Override
	public ElasticsearchSearchQueryIndexScope<?> searchScope() {
		return searchScope;
	}

}
