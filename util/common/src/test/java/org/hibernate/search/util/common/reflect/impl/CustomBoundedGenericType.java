/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.io.Serializable;

@SuppressWarnings("unused")
class CustomBoundedGenericType<T extends Number & Cloneable & Serializable> implements CustomBoundedGenericInterface<T> {
}
