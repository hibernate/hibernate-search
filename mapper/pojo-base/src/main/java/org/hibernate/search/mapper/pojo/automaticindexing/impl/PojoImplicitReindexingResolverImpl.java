/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public class PojoImplicitReindexingResolverImpl<T> implements PojoImplicitReindexingResolver<T> {

	private final PojoPathFilter dirtySelfFilter;
	private final PojoPathFilter dirtySelfOrContainingFilter;
	private final PojoImplicitReindexingResolverNode<T> containingEntitiesResolverRoot;
	private final PojoImplicitReindexingAssociationInverseSideResolver associationInverseSideResolver;

	public PojoImplicitReindexingResolverImpl(PojoPathFilter dirtySelfFilter,
			PojoPathFilter dirtySelfOrContainingFilter,
			PojoImplicitReindexingResolverNode<T> containingEntitiesResolverRoot,
			PojoImplicitReindexingAssociationInverseSideResolver associationInverseSideResolver) {
		this.dirtySelfFilter = dirtySelfFilter;
		this.dirtySelfOrContainingFilter = dirtySelfOrContainingFilter;
		this.containingEntitiesResolverRoot = containingEntitiesResolverRoot;
		this.associationInverseSideResolver = associationInverseSideResolver;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverNode::close, containingEntitiesResolverRoot );
			closer.push( PojoImplicitReindexingAssociationInverseSideResolver::close, associationInverseSideResolver );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "root" );
		builder.attribute( "dirtyPathsTriggeringSelfReindexing", dirtySelfFilter );
		builder.attribute( "associationPaths", associationInverseSideResolver );
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

	@Override
	public PojoImplicitReindexingAssociationInverseSideResolver associationInverseSideResolver() {
		return associationInverseSideResolver;
	}

}
