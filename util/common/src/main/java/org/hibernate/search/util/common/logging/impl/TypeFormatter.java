/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import java.lang.reflect.Type;

public final class TypeFormatter {

	private final Type type;

	public TypeFormatter(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		if ( type instanceof Class ) {
			return ( (Class<?>) type ).getName();
		}
		else if ( type != null ) {
			return type.toString();
		}
		else {
			return "null";
		}
	}
}
