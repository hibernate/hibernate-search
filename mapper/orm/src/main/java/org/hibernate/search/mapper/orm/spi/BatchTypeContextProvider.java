/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.spi;

import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface BatchTypeContextProvider {

	KeyValueProvider<String, ? extends HibernateOrmLoadingTypeContext<?>> byEntityName();

}
