/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.loading.impl;

import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;

public interface PojoSearchLoadingIndexedTypeContext<E> extends PojoLoadingTypeContext<E> {

	IdentifierMapping identifierMapping();

}
