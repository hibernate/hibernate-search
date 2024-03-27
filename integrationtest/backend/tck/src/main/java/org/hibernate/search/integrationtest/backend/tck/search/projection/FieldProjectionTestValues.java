/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class FieldProjectionTestValues<F> extends AbstractProjectionTestValues<F, F> {
	protected FieldProjectionTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
	}

	@Override
	public F fieldValue(int ordinal) {
		return fieldType.valueFromInteger( ordinal );
	}

	@Override
	public F projectedValue(int ordinal) {
		return fieldType.valueFromInteger( ordinal );
	}
}
