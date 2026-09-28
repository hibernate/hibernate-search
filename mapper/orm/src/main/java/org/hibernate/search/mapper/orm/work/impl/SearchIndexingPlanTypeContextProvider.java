package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface SearchIndexingPlanTypeContextProvider {

	<T> PojoTypeContext<T> forExactClass(Class<T> javaClass);

	KeyValueProvider<String, ? extends PojoTypeContext<?>> byEntityName();

}
