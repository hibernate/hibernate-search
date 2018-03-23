/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

class NoOpPojoImplicitReindexingResolver extends PojoImplicitReindexingResolver<Object> {

	private static NoOpPojoImplicitReindexingResolver INSTANCE = new NoOpPojoImplicitReindexingResolver();

	@SuppressWarnings( "unchecked" ) // This instance works for any T
	public static <T> PojoImplicitReindexingResolver<T> get() {
		return (PojoImplicitReindexingResolver<T>) INSTANCE;
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector, Object dirty) {
		// No-op
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
	}
}
