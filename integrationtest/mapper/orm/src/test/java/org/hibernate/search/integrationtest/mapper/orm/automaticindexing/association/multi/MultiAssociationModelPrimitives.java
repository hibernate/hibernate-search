/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.multi;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.AssociationModelPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.MultiValuedPropertyAccessor;

public interface MultiAssociationModelPrimitives<
				TIndexed extends TContaining,
				TContaining,
				TContained,
				TContainedAssociation,
				TContainingAssociation
		> extends AssociationModelPrimitives<TIndexed, TContaining, TContained> {

	@Override
	default boolean isMultiValuedAssociation() {
		return true;
	}

	TContainedAssociation newContainedAssociation(TContainedAssociation original);

	@Override
	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbedded();

	@Override
	MultiValuedPropertyAccessor<TContained, TContaining, TContainingAssociation> containingAsIndexedEmbedded();

	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedNonIndexedEmbedded();

	MultiValuedPropertyAccessor<TContained, TContaining, TContainingAssociation> containingAsNonIndexedEmbedded();

	@Override
	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedShallowReindexOnUpdate();

	@Override
	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedNoReindexOnUpdate();

	@Override
	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedUsedInCrossEntityDerivedProperty();

	@Override
	MultiValuedPropertyAccessor<TContained, TContaining, TContainingAssociation> containingAsUsedInCrossEntityDerivedProperty();

	@Override
	MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedWithCast();

	@Override
	MultiValuedPropertyAccessor<TContained, TContaining, TContainingAssociation> containingAsIndexedEmbeddedWithCast();

}
