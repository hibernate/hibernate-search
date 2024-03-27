/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContext;

/**
 * @param <I> The identifier type for the contained entity type.
 * @param <E> The contained entity type.
 */
public interface PojoScopeContainedTypeContext<I, E>
		extends PojoWorkContainedTypeContext<I, E> {

}
