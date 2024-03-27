/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A model element representing a value bound to a bridge.
 *
 * @see org.hibernate.search.mapper.pojo.bridge.ValueBridge
 */
@Incubating
public interface PojoModelValue<T> extends PojoModelElement {

}
