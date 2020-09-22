/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
