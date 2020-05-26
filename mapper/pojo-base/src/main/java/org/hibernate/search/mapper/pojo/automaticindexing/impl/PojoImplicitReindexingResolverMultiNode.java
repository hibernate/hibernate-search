/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} responsible for applying multiple resolvers.
 *
 * @param <T> The type of "dirty" objects received as input.
 * @param <S> The expected type of the object describing the "dirtiness state".
 */
public class PojoImplicitReindexingResolverMultiNode<T, S> extends PojoImplicitReindexingResolverNode<T, S> {

	private final Collection<? extends PojoImplicitReindexingResolverNode<? super T, S>> elements;

	public PojoImplicitReindexingResolverMultiNode(
			Collection<? extends PojoImplicitReindexingResolverNode<? super T, S>> elements) {
		this.elements = elements;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoImplicitReindexingResolverNode::close, elements );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.startList();
		for ( PojoImplicitReindexingResolverNode<?, ?> element : elements ) {
			builder.value( element );
		}
		builder.endList();
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		for ( PojoImplicitReindexingResolverNode<? super T, S> element : elements ) {
			element.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
		}
	}
}
