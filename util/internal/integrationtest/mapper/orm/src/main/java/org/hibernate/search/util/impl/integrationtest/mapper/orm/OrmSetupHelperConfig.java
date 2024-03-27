/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.BiConsumer;

public interface OrmSetupHelperConfig {

	CoordinationStrategyExpectations coordinationStrategyExpectations();

	default void overrideHibernateSearchDefaults(BiConsumer<String, Object> propertyConsumer) {
	}

}
