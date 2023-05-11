/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingResolverNode} applying a filter to decide whether to apply a delegate.
 * <p>
 * This node allows to optimize reindexing by ignoring some changes when they do not affect a given indexed type.
 *
 * @param <T> The type of "dirty" objects received as input and passed to the delegate.
 */
public class PojoImplicitReindexingResolverDirtinessFilterNode<T> extends PojoImplicitReindexingResolverNode<T> {

	private final PojoPathFilter dirtyPathFilter;
	private final PojoImplicitReindexingResolverNode<T> nested;

	public PojoImplicitReindexingResolverDirtinessFilterNode(PojoPathFilter dirtyPathFilter,
			PojoImplicitReindexingResolverNode<T> nested) {
		Contracts.assertNotNull(
				dirtyPathFilter, "dirtyPathFilter"
		);
		this.dirtyPathFilter = dirtyPathFilter;
		this.nested = nested;
	}

	@Override
	public void close() {
		nested.close();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "reindex only if paths are dirty" );
		appender.attribute( "dirtyPathFilter", dirtyPathFilter );
		appender.attribute( "nested", nested );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		if ( context.isDirtyForReindexingResolution( dirtyPathFilter ) ) {
			nested.resolveEntitiesToReindex( collector, dirty, context );
		}
	}
}
