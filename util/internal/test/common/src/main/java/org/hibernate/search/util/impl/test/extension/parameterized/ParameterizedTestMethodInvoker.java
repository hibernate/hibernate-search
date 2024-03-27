/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

public interface ParameterizedTestMethodInvoker {

	String getName();

	void invoke(Object requiredTestInstance) throws Throwable;
}
