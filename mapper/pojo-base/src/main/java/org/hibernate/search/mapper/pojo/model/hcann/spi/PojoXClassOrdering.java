/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.util.Arrays;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.util.common.reflect.spi.AbstractTypeOrdering;

final class PojoXClassOrdering extends AbstractTypeOrdering<XClass> {

	private final ReflectionManager reflectionManager;

	PojoXClassOrdering(ReflectionManager reflectionManager) {
		this.reflectionManager = reflectionManager;
	}

	@Override
	protected XClass superClass(XClass subType) {
		XClass superClass = subType.getSuperclass();
		if ( superClass == null && subType.isInterface() ) {
			// Make sure Object is considered a superclass of *every* type, even interfaces.
			superClass = reflectionManager.toXClass( Object.class );
		}
		return superClass;
	}

	@Override
	protected Stream<XClass> declaredInterfaces(XClass subType) {
		return Arrays.stream( subType.getInterfaces() );
	}
}
