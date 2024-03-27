/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.spi;

import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.impl.StandalonePojoIntegrationBooterImpl;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

@Incubating
public interface StandalonePojoIntegrationBooter {

	static Builder builder() {
		return new StandalonePojoIntegrationBooterImpl.BuilderImpl();
	}

	interface Builder {
		Builder annotatedTypeSource(AnnotatedTypeSource source);

		Builder valueReadHandleFactory(ValueHandleFactory valueHandleFactory);

		Builder property(String name, Object value);

		Builder properties(Map<String, ?> map);

		StandalonePojoIntegrationBooter build();
	}

	void preBoot(BiConsumer<String, Object> propertyCollector);

	CloseableSearchMapping boot();

}
