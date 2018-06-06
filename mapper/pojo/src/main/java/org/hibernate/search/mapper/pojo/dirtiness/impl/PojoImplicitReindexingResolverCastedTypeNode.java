/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node working at the type level, but applying a cast before anything else.
 * <p>
 * This node may contribute entities to reindex to the collector as well as delegate to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 * <p>
 * This node will ignore entities that cannot be cast to type {@code U}.
 *
 * @param <T> The type of "dirty" objects received as input.
 * @param <U> The type the input objects will be casted to, if possible.
 */
public class PojoImplicitReindexingResolverCastedTypeNode<T, U> extends PojoImplicitReindexingResolver<T> {

	private final PojoCaster<? super U> caster;
	private final boolean shouldMarkForReindexing;
	private final Collection<PojoImplicitReindexingResolver<? super U>> nestedNodes;

	public PojoImplicitReindexingResolverCastedTypeNode(PojoCaster<? super U> caster,
			boolean shouldMarkForReindexing,
			Collection<PojoImplicitReindexingResolver<? super U>> nestedNodes) {
		this.caster = caster;
		this.shouldMarkForReindexing = shouldMarkForReindexing;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "caster", caster );
		builder.attribute( "shouldMarkForReindexing", shouldMarkForReindexing );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolver<?> node : nestedNodes ) {
			builder.value( node );
		}
		builder.endList();
	}

	@Override
	@SuppressWarnings( "unchecked" ) // We can only cast to the raw type, if U is generic we need an unchecked cast
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, PojoDirtinessState dirtinessState) {
		U castedDirty = (U) caster.castOrNull( runtimeIntrospector.unproxy( dirty ) );
		if ( castedDirty != null ) {
			if ( shouldMarkForReindexing ) {
				collector.markForReindexing( castedDirty );
			}
			for ( PojoImplicitReindexingResolver<? super U> node : nestedNodes ) {
				node.resolveEntitiesToReindex( collector, runtimeIntrospector, castedDirty, dirtinessState );
			}
		}
	}
}
