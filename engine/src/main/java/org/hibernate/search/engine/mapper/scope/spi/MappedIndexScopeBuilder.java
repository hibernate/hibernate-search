/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.scope.spi;


public interface MappedIndexScopeBuilder<R, E> {

	MappedIndexScope<R, E> build();

}
