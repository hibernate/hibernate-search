/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaFieldNode<F> {

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final ElasticsearchFieldCodec<F> codec;

	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;

	private final ElasticsearchFieldSortBuilderFactory sortBuilderFactory;

	private final ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory) {
		this.parent = parent;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public ElasticsearchFieldCodec<F> getCodec() {
		return codec;
	}

	public ElasticsearchFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public ElasticsearchFieldSortBuilderFactory getSortBuilderFactory() {
		return sortBuilderFactory;
	}

	public ElasticsearchFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", predicateBuilderFactory=" ).append( predicateBuilderFactory )
				.append( ", sortBuilderFactory=" ).append( sortBuilderFactory )
				.append( ", projectionBuilderFactory=" ).append( projectionBuilderFactory )
				.append( "]" );
		return sb.toString();
	}
}
