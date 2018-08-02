/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaFieldNode<F> {

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final ElasticsearchFieldConverter converter;
	private final ElasticsearchFieldCodec<F> codec;

	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent,
			ElasticsearchFieldConverter converter,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory) {
		this.parent = parent;
		this.converter = converter;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public ElasticsearchFieldConverter getConverter() {
		return converter;
	}

	public ElasticsearchFieldCodec<F> getCodec() {
		return codec;
	}

	public ElasticsearchFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public boolean isCompatibleWith(ElasticsearchIndexSchemaFieldNode<?> other) {
		return converter.isDslCompatibleWith( other.converter )
				&& Objects.equals( codec, other.codec )
				&& predicateBuilderFactory.isDslCompatibleWith( other.predicateBuilderFactory );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", converter=" ).append( converter )
				.append( ", predicateBuilderFactory=" ).append( predicateBuilderFactory )
				.append( "]" );
		return sb.toString();
	}
}
