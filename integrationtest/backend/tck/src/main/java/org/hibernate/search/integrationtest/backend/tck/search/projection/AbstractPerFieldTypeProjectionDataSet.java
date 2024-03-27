/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public abstract class AbstractPerFieldTypeProjectionDataSet<F, P, V extends AbstractProjectionTestValues<F, P>>
		extends AbstractProjectionDataSet {

	protected final FieldTypeDescriptor<F, ?> fieldType;
	protected final V values;

	protected AbstractPerFieldTypeProjectionDataSet(String routingKey, V values) {
		super( routingKey );
		fieldType = values.fieldType();
		this.values = values;
	}

}
