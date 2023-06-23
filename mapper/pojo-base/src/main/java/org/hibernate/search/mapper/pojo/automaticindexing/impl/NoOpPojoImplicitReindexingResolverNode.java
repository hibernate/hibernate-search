/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.util.common.spi.ToStringTreeAppender;

class NoOpPojoImplicitReindexingResolverNode extends PojoImplicitReindexingResolverNode<Object> {

	private static final NoOpPojoImplicitReindexingResolverNode INSTANCE = new NoOpPojoImplicitReindexingResolverNode();

	@SuppressWarnings("unchecked") // This instance works for any T
	public static <T> PojoImplicitReindexingResolverNode<T> get() {
		return (PojoImplicitReindexingResolverNode<T>) INSTANCE;
	}

	@Override
	public void close() {
		// No-op
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			Object dirty,
			PojoImplicitReindexingResolverRootContext context) {
		// No-op
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "no op" );
	}
}
