/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

public interface PojoTypeContext<T> {

	PojoRawTypeIdentifier<T> typeIdentifier();

}
