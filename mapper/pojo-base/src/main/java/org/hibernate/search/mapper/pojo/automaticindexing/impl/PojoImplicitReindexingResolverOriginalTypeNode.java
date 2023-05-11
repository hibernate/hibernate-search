/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingResolverNode} working at the type level, without casting.
 * <p>
 * This node may delegate to a {@link PojoImplicitReindexingResolverMarkingNode marking node}
 * to mark the input as "to reindex" as well as delegate  to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 *
 * @param <T> The type of "dirty" objects received as input.
 */
public class PojoImplicitReindexingResolverOriginalTypeNode<T> extends PojoImplicitReindexingResolverNode<T> {

	private final PojoImplicitReindexingResolverNode<? super T> nested;

	public PojoImplicitReindexingResolverOriginalTypeNode(PojoImplicitReindexingResolverNode<? super T> nested) {
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
		appender.attribute( "operation", "process type" );
		appender.attribute( "nested", nested );
	}

	@Override
	@SuppressWarnings("unchecked") // As long as T is not a proxy-specific interface, it will also be implemented by the unproxified object
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		dirty = (T) context.sessionContext().runtimeIntrospector().unproxy( dirty );
		nested.resolveEntitiesToReindex( collector, dirty, context );
	}
}
