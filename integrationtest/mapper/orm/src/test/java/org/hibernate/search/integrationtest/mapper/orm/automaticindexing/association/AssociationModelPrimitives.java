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

	SingleValuedPropertyAccessor<TContaining, String> containingEntityNonIndexedField();

	SingleValuedPropertyAccessor<TContaining, TContaining> child();

	SingleValuedPropertyAccessor<TContaining, TContaining> parent();

	PropertyAccessor<TContaining, TContained> containedIndexedEmbedded();

	PropertyAccessor<TContained, TContaining> containingAsIndexedEmbedded();

	PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedShallowReindexOnUpdate();

	PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedNoReindexOnUpdate();

	PropertyAccessor<TContaining, TContained> containedUsedInCrossEntityDerivedProperty();

	PropertyAccessor<TContained, TContaining> containingAsUsedInCrossEntityDerivedProperty();

	PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedWithCast();

	PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedWithCast();

	SingleValuedPropertyAccessor<TContained, String> indexedField();

	SingleValuedPropertyAccessor<TContained, String> nonIndexedField();

	MultiValuedPropertyAccessor<TContained, String, List<String>> indexedElementCollectionField();

	MultiValuedPropertyAccessor<TContained, String, List<String>> nonIndexedElementCollectionField();

	SingleValuedPropertyAccessor<TContained, String> fieldUsedInContainedDerivedField1();

	SingleValuedPropertyAccessor<TContained, String> fieldUsedInContainedDerivedField2();

	SingleValuedPropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField1();

	SingleValuedPropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField2();
}
