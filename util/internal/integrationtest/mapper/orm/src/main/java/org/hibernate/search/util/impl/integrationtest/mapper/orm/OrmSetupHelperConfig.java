package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.BiConsumer;

public interface OrmSetupHelperConfig {

	CoordinationStrategyExpectations coordinationStrategyExpectations();

	default void overrideHibernateSearchDefaults(BiConsumer<String, Object> propertyConsumer) {
	}

}
