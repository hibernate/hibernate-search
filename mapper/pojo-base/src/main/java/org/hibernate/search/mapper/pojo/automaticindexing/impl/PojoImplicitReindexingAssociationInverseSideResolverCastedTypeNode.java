/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingAssociationInverseSideResolverNode} working at the type level,
 * but applying a cast before anything else.
 * <p>
 * This node will ignore entities that cannot be cast to type {@code U}.
 *
 * @param <T> The type of "dirty" objects received as input.
 * @param <U> The type the input objects will be casted to, if possible.
 */
class PojoImplicitReindexingAssociationInverseSideResolverCastedTypeNode<T, U>
		extends PojoImplicitReindexingAssociationInverseSideResolverNode<T> {

	private final PojoCaster<? super U> caster;
	private final PojoImplicitReindexingAssociationInverseSideResolverNode<? super U> nested;

	public PojoImplicitReindexingAssociationInverseSideResolverCastedTypeNode(PojoCaster<? super U> caster,
			PojoImplicitReindexingAssociationInverseSideResolverNode<? super U> nested) {
		this.caster = caster;
		this.nested = nested;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process type (with cast, ignore if it fails)" );
		builder.attribute( "caster", caster );
		builder.attribute( "nested", nested );
	}

	@Override
	void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector, T state,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context) {
		// The caster can only cast to the raw type, beyond that we have to use an unchecked cast.
		@SuppressWarnings("unchecked")
		U castedDirty = (U) caster.castOrNull( context.runtimeIntrospector().unproxy( state ) );
		if ( castedDirty != null ) {
			nested.resolveEntitiesToReindex( collector, castedDirty, context );
		}
	}
}
