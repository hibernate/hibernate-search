/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import java.util.List;

public interface AssociationModelPrimitives<
				TIndexed extends TContaining,
				TContaining,
				TContained
		> {
	String getIndexName();

	boolean isMultiValuedAssociation();

	Class<TIndexed> getIndexedClass();

	Class<TContaining> getContainingClass();

	Class<TContained> getContainedClass();

	TIndexed newIndexed(int id);

	TContaining newContaining(int id);

	TContained newContained(int id);

	void setContainingEntityNonIndexedField(TContaining containing, String value);

	void setChild(TContaining parent, TContaining child);

	void setParent(TContaining child, TContaining parent);

	void setContainedIndexedEmbeddedSingle(TContaining containing, TContained contained);

	void setContainingAsIndexedEmbeddedSingle(TContained contained, TContaining containing);

	void setContainedIndexedEmbeddedShallowReindexOnUpdateSingle(TContaining containing, TContained contained);

	void setContainedIndexedEmbeddedNoReindexOnUpdateSingle(TContaining containing, TContained contained);

	void setContainedUsedInCrossEntityDerivedPropertySingle(TContaining containing, TContained contained);

	void setContainingAsUsedInCrossEntityDerivedPropertySingle(TContained contained, TContaining containing);

	void setContainedIndexedEmbeddedWithCastSingle(TContaining containing, TContained contained);

	void setContainingAsIndexedEmbeddedWithCastSingle(TContained contained, TContaining containing);

	void setIndexedField(TContained contained, String value);

	void setNonIndexedField(TContained contained, String value);

	List<String> getIndexedElementCollectionField(TContained contained);

	void setIndexedElementCollectionField(TContained contained, List<String> value);

	List<String> getNonIndexedElementCollectionField(TContained contained);

	void setNonIndexedElementCollectionField(TContained contained, List<String> value);

	void setFieldUsedInContainedDerivedField1(TContained contained, String value);

	void setFieldUsedInContainedDerivedField2(TContained contained, String value);

	void setFieldUsedInCrossEntityDerivedField1(TContained contained, String value);

	void setFieldUsedInCrossEntityDerivedField2(TContained contained, String value);
}
