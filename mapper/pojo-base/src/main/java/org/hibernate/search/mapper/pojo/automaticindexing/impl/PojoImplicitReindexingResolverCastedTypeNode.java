/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

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
 * @param <U> The type the input objects will be casted to, if possible.
 */
public class PojoImplicitReindexingResolverCastedTypeNode<T, U> extends PojoImplicitReindexingResolverNode<T> {

	private final PojoCaster<? super U> caster;
	private final PojoImplicitReindexingResolverNode<? super U> nested;

	public PojoImplicitReindexingResolverCastedTypeNode(PojoCaster<? super U> caster,
			PojoImplicitReindexingResolverNode<? super U> nested) {
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
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process type (with cast, ignore if it fails)" );
		appender.attribute( "caster", caster );
		appender.attribute( "nested", nested );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		// The caster can only cast to the raw type, beyond that we have to use an unchecked cast.
		@SuppressWarnings("unchecked")
		U castedDirty = (U) caster.castOrNull( context.sessionContext().runtimeIntrospector().unproxy( dirty ) );
		if ( castedDirty != null ) {
			nested.resolveEntitiesToReindex( collector, castedDirty, context );
		}
	}
}
