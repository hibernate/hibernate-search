/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Collection;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} responsible for applying multiple resolvers.
 *
 * @param <T> The type of "dirty" objects received as input.
 */
public class PojoImplicitReindexingResolverMultiNode<T> extends PojoImplicitReindexingResolverNode<T> {

	private final Collection<? extends PojoImplicitReindexingResolverNode<? super T>> elements;

	public PojoImplicitReindexingResolverMultiNode(
			Collection<? extends PojoImplicitReindexingResolverNode<? super T>> elements) {
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
		builder.attribute( null, elements );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		for ( PojoImplicitReindexingResolverNode<? super T> element : elements ) {
			element.resolveEntitiesToReindex( collector, dirty, context );
		}
	}
}
