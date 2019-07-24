/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} working at the type level, without casting.
 * <p>
 * This node may delegate to a {@link PojoImplicitReindexingResolverMarkingNode marking node}
 * to mark the input as "to reindex" as well as delegate  to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 *
 * @param <T> The type of "dirty" objects received as input.
 * @param <S> The expected type of the object describing the "dirtiness state".
 */
public class PojoImplicitReindexingResolverOriginalTypeNode<T, S> extends PojoImplicitReindexingResolverNode<T, S> {

	private final Collection<PojoImplicitReindexingResolverNode<? super T, S>> nestedNodes;

	public PojoImplicitReindexingResolverOriginalTypeNode(
			Collection<PojoImplicitReindexingResolverNode<? super T, S>> nestedNodes) {
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoImplicitReindexingResolverNode::close, nestedNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolverNode<?, ?> node : nestedNodes ) {
			builder.value( node );
		}
		builder.endList();
	}

	@Override
	@SuppressWarnings("unchecked") // As long as T is not a proxy-specific interface, it will also be implemented by the unproxified object
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		dirty = (T) runtimeIntrospector.unproxy( dirty );
		for ( PojoImplicitReindexingResolverNode<? super T, S> node : nestedNodes ) {
			node.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
		}
	}
}
