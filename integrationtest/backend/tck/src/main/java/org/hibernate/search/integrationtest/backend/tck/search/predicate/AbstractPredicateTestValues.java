/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
