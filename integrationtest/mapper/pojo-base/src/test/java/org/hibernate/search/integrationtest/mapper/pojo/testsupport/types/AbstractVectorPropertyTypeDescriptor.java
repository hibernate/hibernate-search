/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

public abstract class AbstractVectorPropertyTypeDescriptor<V, F> extends PropertyTypeDescriptor<V, F> {

	protected AbstractVectorPropertyTypeDescriptor(Class<V> javaType) {
		super( javaType );
	}

	@Override
	public boolean isVectorType() {
		return true;
	}

	@Override
	public boolean supportedAsIdentifier() {
		return false;
	}
}
