/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public abstract class AbstractPerFieldTypePredicateDataSet<F, V extends AbstractPredicateTestValues<F>>
		extends AbstractPredicateDataSet {

	protected final FieldTypeDescriptor<F, ?> fieldType;
	protected final V values;

	protected AbstractPerFieldTypePredicateDataSet(V values) {
		super( values.fieldType().getUniqueName() );
		fieldType = values.fieldType();
		this.values = values;
	}

}
