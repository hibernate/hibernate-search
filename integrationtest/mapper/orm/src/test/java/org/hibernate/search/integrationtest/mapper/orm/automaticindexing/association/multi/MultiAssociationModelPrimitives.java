/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.multi;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.AssociationModelPrimitives;

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

	@Override
	default void setContainedIndexedEmbeddedSingle(TContaining containing, TContained contained) {
		TContainedAssociation containedAssociation = getContainedIndexedEmbedded( containing );
		clearContained( containedAssociation );
		addContained( containedAssociation, contained );
	}

	@Override
	default void setContainingAsIndexedEmbeddedSingle(TContained contained, TContaining containing) {
		TContainingAssociation containingAssociation = getContainingAsIndexedEmbedded( contained );
		clearContaining( containingAssociation );
		addContaining( containingAssociation, containing );
	}

	@Override
	default void setContainedIndexedEmbeddedNoReindexOnUpdateSingle(TContaining containing, TContained contained) {
		TContainedAssociation containedAssociation = getContainedIndexedEmbeddedNoReindexOnUpdate( containing );
		clearContained( containedAssociation );
		addContained( containedAssociation, contained );
	}

	@Override
	default void setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle(TContained contained, TContaining containing) {
		TContainingAssociation containingAssociation = getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained );
		clearContaining( containingAssociation );
		addContaining( containingAssociation, containing );
	}

	@Override
	default void setContainedUsedInCrossEntityDerivedPropertySingle(TContaining containing,
			TContained contained) {
		TContainedAssociation containedAssociation = getContainedUsedInCrossEntityDerivedProperty( containing );
		clearContained( containedAssociation );
		addContained( containedAssociation, contained );
	}

	@Override
	default void setContainingAsUsedInCrossEntityDerivedPropertySingle(TContained contained,
			TContaining containing) {
		TContainingAssociation containingAssociation = getContainingAsUsedInCrossEntityDerivedProperty( contained );
		clearContaining( containingAssociation );
		addContaining( containingAssociation, containing );
	}

	@Override
	default void setContainedIndexedEmbeddedWithCastSingle(TContaining containing, TContained contained) {
		TContainedAssociation containedAssociation = getContainedIndexedEmbeddedWithCast( containing );
		clearContained( containedAssociation );
		addContained( containedAssociation, contained );
	}

	@Override
	default void setContainingAsIndexedEmbeddedWithCastSingle(TContained contained, TContaining containing) {
		TContainingAssociation containingAssociation = getContainingAsIndexedEmbeddedWithCast( contained );
		clearContaining( containingAssociation );
		addContaining( containingAssociation, containing );
	}

	TContainedAssociation newContainedAssociation(TContainedAssociation original);

	void addContained(TContainedAssociation association, TContained contained);

	void removeContained(TContainedAssociation association, TContained contained);

	void clearContained(TContainedAssociation association);

	void addContaining(TContainingAssociation association, TContaining containing);

	void removeContaining(TContainingAssociation association, TContaining containing);

	void clearContaining(TContainingAssociation association);

	TContainedAssociation getContainedIndexedEmbedded(TContaining containing);

	void setContainedIndexedEmbedded(TContaining containing, TContainedAssociation association);

	TContainingAssociation getContainingAsIndexedEmbedded(TContained contained);

	TContainedAssociation getContainedNonIndexedEmbedded(TContaining containing);

	void setContainedNonIndexedEmbedded(TContaining containing, TContainedAssociation association);

	TContainingAssociation getContainingAsNonIndexedEmbedded(TContained contained);

	TContainedAssociation getContainedIndexedEmbeddedNoReindexOnUpdate(TContaining containing);

	void setContainedIndexedEmbeddedNoReindexOnUpdate(TContaining containing, TContainedAssociation association);

	TContainingAssociation getContainingAsIndexedEmbeddedNoReindexOnUpdate(TContained contained);

	TContainedAssociation getContainedUsedInCrossEntityDerivedProperty(TContaining containing);

	TContainingAssociation getContainingAsUsedInCrossEntityDerivedProperty(TContained contained);

	TContainedAssociation getContainedIndexedEmbeddedWithCast(TContaining containing);

	TContainingAssociation getContainingAsIndexedEmbeddedWithCast(TContained contained);
}
