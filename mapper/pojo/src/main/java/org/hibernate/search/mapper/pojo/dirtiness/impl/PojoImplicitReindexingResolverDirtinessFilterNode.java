/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node applying a filter to decide whether to apply a delegate.
 * <p>
 * This node allows to optimize reindexing by ignoring some changes when they do not affect a given indexed type.
 *
 * @param <T> The type of "dirty" objects received as input and passed to the delegate.
 */
public class PojoImplicitReindexingResolverDirtinessFilterNode<T> extends PojoImplicitReindexingResolver<T> {

	private final Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexing;
	private final PojoImplicitReindexingResolver<T> delegate;

	public PojoImplicitReindexingResolverDirtinessFilterNode(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexing,
			PojoImplicitReindexingResolver<T> delegate) {
		Contracts.assertNotNullNorEmpty(
				dirtyPathsTriggeringReindexing, "dirtyPathsTriggeringReindexing"
		);
		this.dirtyPathsTriggeringReindexing = dirtyPathsTriggeringReindexing;
		this.delegate = delegate;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "dirtyPathsTriggeringReindexing", dirtyPathsTriggeringReindexing );
		builder.attribute( "delegate", delegate );
	}

	@Override
	@SuppressWarnings( "unchecked" ) // We can only cast to the raw type, if U is generic we need an unchecked cast
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, PojoDirtinessState dirtinessState) {
		if ( dirtinessState.isAnyDirty( dirtyPathsTriggeringReindexing ) ) {
			delegate.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
		}
	}
}
