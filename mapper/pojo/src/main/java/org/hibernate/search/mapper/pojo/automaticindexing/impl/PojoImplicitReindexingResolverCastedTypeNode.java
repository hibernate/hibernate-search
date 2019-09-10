/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} working at the type level, but applying a cast before anything else.
 * <p>
 * This node may delegate to a {@link PojoImplicitReindexingResolverMarkingNode marking node}
 * to mark the input as "to reindex" as well as delegate to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 * <p>
 * This node will ignore entities that cannot be cast to type {@code U}.
 *
 * @param <T> The type of "dirty" objects received as input.
 * @param <S> The expected type of the object describing the "dirtiness state".
 * @param <U> The type the input objects will be casted to, if possible.
 */
public class PojoImplicitReindexingResolverCastedTypeNode<T, S, U> extends PojoImplicitReindexingResolverNode<T, S> {

	private final PojoCaster<? super U> caster;
	private final Collection<PojoImplicitReindexingResolverNode<? super U, S>> nestedNodes;

	public PojoImplicitReindexingResolverCastedTypeNode(PojoCaster<? super U> caster,
			Collection<PojoImplicitReindexingResolverNode<? super U, S>> nestedNodes) {
		this.caster = caster;
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
		builder.attribute( "caster", caster );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolverNode<?, ?> node : nestedNodes ) {
			builder.value( node );
		}
		builder.endList();
	}

	@Override
	@SuppressWarnings( "unchecked" ) // We can only cast to the raw type, if U is generic we need an unchecked cast
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		U castedDirty = (U) caster.castOrNull( runtimeIntrospector.unproxy( dirty ) );
		if ( castedDirty != null ) {
			for ( PojoImplicitReindexingResolverNode<? super U, S> node : nestedNodes ) {
				node.resolveEntitiesToReindex( collector, runtimeIntrospector, castedDirty, dirtinessState );
			}
		}
	}
}
