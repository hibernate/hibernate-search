/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

class NoOpPojoImplicitReindexingResolver extends PojoImplicitReindexingResolver<Object, Object> {

	private static NoOpPojoImplicitReindexingResolver INSTANCE = new NoOpPojoImplicitReindexingResolver();

	@SuppressWarnings( "unchecked" ) // This instance works for any T or D
	public static <T, D> PojoImplicitReindexingResolver<T, D> get() {
		return (PojoImplicitReindexingResolver<T, D>) INSTANCE;
	}

	@Override
	public boolean requiresSelfReindexing(Object dirtinessState) {
		return false;
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, Object dirty, Object dirtinessState) {
		// No-op
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
	}
}
