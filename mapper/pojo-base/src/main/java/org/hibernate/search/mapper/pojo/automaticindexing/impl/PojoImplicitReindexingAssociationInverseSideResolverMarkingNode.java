/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinalReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingAssociationInverseSideResolverNode} marking as "to reindex"
 * object passed as an input.
 */
public class PojoImplicitReindexingAssociationInverseSideResolverMarkingNode
		extends PojoImplicitReindexingAssociationInverseSideResolverNode<Object> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeIdentifier<?>, PojoPathOrdinalReference> inverseSidePathOrdinalByType;

	public PojoImplicitReindexingAssociationInverseSideResolverMarkingNode(
			Map<PojoRawTypeIdentifier<?>, PojoPathOrdinalReference> inverseSidePathOrdinalByType) {
		this.inverseSidePathOrdinalByType = inverseSidePathOrdinalByType;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "mark association inverse side as dirty" );
		appender.attribute( "inverseSidePathOrdinalByType", inverseSidePathOrdinalByType );
	}

	@Override
	void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector, Object entity,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context) {
		if ( entity == null ) {
			// There's nothing to reindex.
			return;
		}
		PojoRawTypeIdentifier<?> entityTypeIdentifier = context.detectContainingEntityType( entity );
		PojoPathOrdinalReference inverseSidePathOrdinal = inverseSidePathOrdinalByType.get( entityTypeIdentifier );
		if ( inverseSidePathOrdinal == null ) {
			// This can happen when we have inheritance hierarchy in play:
			// 	Assume having an entity A extended by an indexed entity B. And A has an association to A,e.g.:
			//
			// 	class A {
			// 		A a;
			// 	}
			//	@Indexed class B extends A {
			//	}
			//
			//	Now in this scenario we would care if an actual instance of `A a` is B. As that would be when `a` has to be re-indexed.
			//	This means that the inverseSidePathOrdinalByType would contain only the key for `B` type, but not for `A`.
			//	At runtime, when an actual instance of `A a` is B all good we find a inverse side in the map.
			//	Otherwise, if the `A a` is an instance of A then the map won't have a reference, as we do not care about such scenario, `A` is not indexed.
			//
			//	And if that happens we just want to return fast without updating the association.
			return;
		}
		collector.updateBecauseOfContainedAssociation( entityTypeIdentifier, entity, inverseSidePathOrdinal.ordinal );
	}

}
