/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

interface ArrayModelPrimitives<TIndexed, TArray, TIndexField> {
	String getIndexName();

	Class<TIndexed> getIndexedClass();

	TIndexed newIndexed(int id);

	TArray newArray(int size);

	void setElement(TArray array, int arrayIndex, int valueOrdinal);

	Class<TIndexField> getExpectedIndexFieldType();

	TIndexField getExpectedIndexFieldValue(TArray array, int arrayIndex);

	void setSerializedArray(TIndexed indexed, TArray array);

	TArray getSerializedArray(TIndexed indexed);

	void setElementCollectionArray(TIndexed indexed, TArray array);

	TArray getElementCollectionArray(TIndexed indexed);
}
