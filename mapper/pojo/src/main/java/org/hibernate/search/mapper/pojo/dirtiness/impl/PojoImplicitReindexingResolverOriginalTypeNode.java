/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node working at the type level, without casting.
 * <p>
 * This node may contribute entities to reindex to the collector as well as delegate to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 *
 * @param <T> The type of "dirty" objects received as input.
 */
public class PojoImplicitReindexingResolverOriginalTypeNode<T> extends PojoImplicitReindexingResolver<T> {

	private final boolean shouldMarkForReindexing;
	private final Collection<PojoImplicitReindexingResolver<? super T>> nestedNodes;

	public PojoImplicitReindexingResolverOriginalTypeNode(boolean shouldMarkForReindexing,
			Collection<PojoImplicitReindexingResolver<? super T>> nestedNodes) {
		this.shouldMarkForReindexing = shouldMarkForReindexing;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "shouldMarkForReindexing", shouldMarkForReindexing );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolver<?> node : nestedNodes ) {
			builder.value( node );
		}
		builder.endList();
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, PojoDirtinessState dirtinessState) {
		if ( shouldMarkForReindexing ) {
			collector.markForReindexing( dirty );
		}
		for ( PojoImplicitReindexingResolver<? super T> node : nestedNodes ) {
			node.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
		}
	}
}
