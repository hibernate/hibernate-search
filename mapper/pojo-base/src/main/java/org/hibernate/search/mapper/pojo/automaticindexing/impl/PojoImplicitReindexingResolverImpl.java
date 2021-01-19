/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public class PojoImplicitReindexingResolverImpl<T> implements PojoImplicitReindexingResolver<T> {

	private final PojoPathFilter dirtySelfFilter;
	private final PojoPathFilter dirtySelfOrContainingFilter;
	private final PojoImplicitReindexingResolverNode<T> containingEntitiesResolverRoot;

	public PojoImplicitReindexingResolverImpl(PojoPathFilter dirtySelfFilter,
			PojoPathFilter dirtySelfOrContainingFilter,
			PojoImplicitReindexingResolverNode<T> containingEntitiesResolverRoot) {
		this.dirtySelfFilter = dirtySelfFilter;
		this.dirtySelfOrContainingFilter = dirtySelfOrContainingFilter;
		this.containingEntitiesResolverRoot = containingEntitiesResolverRoot;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void close() {
		containingEntitiesResolverRoot.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "root" );
		builder.attribute( "dirtyPathsTriggeringSelfReindexing", dirtySelfFilter );
		builder.attribute( "containingEntitiesResolverRoot", containingEntitiesResolverRoot );
	}

	@Override
	public PojoPathFilter dirtySelfFilter() {
		return dirtySelfFilter;
	}

	@Override
	public PojoPathFilter dirtySelfOrContainingFilter() {
		return dirtySelfOrContainingFilter;
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		containingEntitiesResolverRoot.resolveEntitiesToReindex( collector, dirty, context );
	}

}
