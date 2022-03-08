/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

public abstract class AbstractElasticsearchProjection<P> implements ElasticsearchSearchProjection<P> {

	protected final Set<String> indexNames;

	protected AbstractElasticsearchProjection(AbstractBuilder<?> builder) {
		this( builder.scope );
	}

	protected AbstractElasticsearchProjection(ElasticsearchSearchIndexScope<?> scope) {
		indexNames = scope.hibernateSearchIndexNames();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder<P> implements SearchProjectionBuilder<P> {

		protected final ElasticsearchSearchIndexScope<?> scope;

		protected AbstractBuilder(ElasticsearchSearchIndexScope<?> scope) {
			this.scope = scope;
		}
	}
}
