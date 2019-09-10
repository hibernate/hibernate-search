/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} applying a filter to decide whether to apply a delegate.
 * <p>
 * This node allows to optimize reindexing by ignoring some changes when they do not affect a given indexed type.
 *
 * @param <T> The type of "dirty" objects received as input and passed to the delegate.
 * @param <S> The expected type of the object describing the "dirtiness state".
 */
public class PojoImplicitReindexingResolverDirtinessFilterNode<T, S> extends PojoImplicitReindexingResolverNode<T, S> {

	private final PojoPathFilter<S> dirtyPathFilter;
	private final PojoImplicitReindexingResolverNode<T, S> delegate;

	public PojoImplicitReindexingResolverDirtinessFilterNode(PojoPathFilter<S> dirtyPathFilter,
			PojoImplicitReindexingResolverNode<T, S> delegate) {
		Contracts.assertNotNull(
				dirtyPathFilter, "dirtyPathFilter"
		);
		this.dirtyPathFilter = dirtyPathFilter;
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "dirtyPathFilter", dirtyPathFilter );
		builder.attribute( "delegate", delegate );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		// See method javadoc: null means we must consider all paths as dirty
		if ( dirtinessState == null || dirtyPathFilter.test( dirtinessState ) ) {
			delegate.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
		}
	}
}
