/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

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
		return toStringTree();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverNode::close, containingEntitiesResolverRoot );
			closer.push( PojoImplicitReindexingAssociationInverseSideResolver::close, associationInverseSideResolver );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "root" );
		appender.attribute( "dirtyPathsTriggeringSelfReindexing", dirtySelfFilter );
		appender.attribute( "associationPaths", associationInverseSideResolver );
		appender.attribute( "containingEntitiesResolverRoot", containingEntitiesResolverRoot );
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
