/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

/**
 * @param <F> The type of field values.
 */
public abstract class AbstractPredicateTestValues<F> {

	protected final FieldTypeDescriptor<F, ?> fieldType;

	protected AbstractPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		this.fieldType = fieldType;
	}

	public FieldTypeDescriptor<F, ?> fieldType() {
		return fieldType;
	}

	public abstract F fieldValue(int docOrdinal);

	public abstract int size();

}
