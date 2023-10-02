/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public interface PojoModelPathWalker<C, T, P, V> {

	P property(C context, T typeNode, PojoModelPathPropertyNode pathNode);

	V value(C context, P propertyNode, PojoModelPathValueNode pathNode);

	T type(C context, V valueNode);

}
