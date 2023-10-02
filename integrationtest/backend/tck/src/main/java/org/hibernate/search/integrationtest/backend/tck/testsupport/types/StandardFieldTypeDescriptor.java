/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public abstract class StandardFieldTypeDescriptor<F>
		extends FieldTypeDescriptor<F, StandardIndexFieldTypeOptionsStep<?, F>> {

	protected StandardFieldTypeDescriptor(Class<F> javaType) {
		super( javaType );
	}

	protected StandardFieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		super( javaType, uniqueName );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.as( javaType );
	}
}
