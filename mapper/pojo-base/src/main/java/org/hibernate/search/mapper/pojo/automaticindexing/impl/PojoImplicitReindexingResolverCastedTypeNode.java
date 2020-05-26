/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

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
	private final PojoImplicitReindexingResolverNode<? super U, S> nested;

	public PojoImplicitReindexingResolverCastedTypeNode(PojoCaster<? super U> caster,
			PojoImplicitReindexingResolverNode<? super U, S> nested) {
		this.caster = caster;
		this.nested = nested;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverNode::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process type (with cast)" );
		builder.attribute( "caster", caster );
		builder.attribute( "nested", nested );
	}

	@Override
	@SuppressWarnings( "unchecked" ) // We can only cast to the raw type, if U is generic we need an unchecked cast
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, S dirtinessState) {
		U castedDirty = (U) caster.castOrNull( runtimeIntrospector.unproxy( dirty ) );
		if ( castedDirty != null ) {
			nested.resolveEntitiesToReindex( collector, runtimeIntrospector, castedDirty, dirtinessState );
		}
	}
}
