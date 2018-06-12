/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

public class PojoImplicitReindexingResolverImpl<T, S> extends PojoImplicitReindexingResolver<T, S> {

	private final PojoPathFilter<S> dirtyPathsTriggeringSelfReindexing;
	private final PojoImplicitReindexingResolverNode<T, S> containingEntitiesResolverRoot;

	public PojoImplicitReindexingResolverImpl(
			PojoPathFilter<S> dirtyPathsTriggeringSelfReindexing,
			PojoImplicitReindexingResolverNode<T, S> containingEntitiesResolverRoot) {
		this.dirtyPathsTriggeringSelfReindexing = dirtyPathsTriggeringSelfReindexing;
		this.containingEntitiesResolverRoot = containingEntitiesResolverRoot;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "dirtyPathsTriggeringSelfReindexing", dirtyPathsTriggeringSelfReindexing );
		builder.attribute( "containingEntitiesResolverRoot", containingEntitiesResolverRoot );
	}

	@Override
	public boolean requiresSelfReindexing(S dirtinessState) {
		return dirtinessState == null || dirtyPathsTriggeringSelfReindexing.test( dirtinessState );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		containingEntitiesResolverRoot.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty, dirtinessState );
	}

}
