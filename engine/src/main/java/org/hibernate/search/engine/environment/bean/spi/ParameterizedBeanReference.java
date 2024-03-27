/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.spi;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;

public final class ParameterizedBeanReference<T> {
	public static <T> ParameterizedBeanReference<T> of(BeanReference<T> reference, Map<String, ?> params) {
		return new ParameterizedBeanReference<>( reference, params );
	}

	private final BeanReference<T> reference;
	private final Map<String, ?> params;

	private ParameterizedBeanReference(BeanReference<T> reference, Map<String, ?> params) {
		this.reference = reference;
		this.params = params;
	}

	public BeanReference<T> reference() {
		return reference;
	}

	public Map<String, ?> params() {
		return params;
	}
}
