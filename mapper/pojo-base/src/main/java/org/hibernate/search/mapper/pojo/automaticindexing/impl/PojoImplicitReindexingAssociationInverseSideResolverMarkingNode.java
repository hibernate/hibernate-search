/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinalReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingAssociationInverseSideResolverNode} marking as "to reindex"
 * object passed as an input.
 */
public class PojoImplicitReindexingAssociationInverseSideResolverMarkingNode
		extends PojoImplicitReindexingAssociationInverseSideResolverNode<Object> {

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
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "mark association inverse side as dirty" );
		builder.attribute( "inverseSidePathOrdinalByType", inverseSidePathOrdinalByType );
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
			// This shouldn't happen, as this means we encountered an unexpected entity type
			// as the target of the association.
			// We're ignoring the problem instead of throwing an exception for backwards compatibility,
			// as we don't want this feature to cause errors in existing applications.
			// TODO HSEARCH-4720 when we can afford breaking changes (in the next major), we should probably throw an exception here?
			return;
		}
		collector.updateBecauseOfContainedAssociation( entityTypeIdentifier, entity, inverseSidePathOrdinal.ordinal );
	}

}
