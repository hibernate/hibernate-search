/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.spi;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
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

		@Incubating
		Builder introspectorCustomizer(Function<PojoBootstrapIntrospector, PojoBootstrapIntrospector> customize);

		Builder property(String name, Object value);

		Builder properties(Map<String, ?> map);

		StandalonePojoIntegrationBooter build();
	}

	void preBoot(BiConsumer<String, Object> propertyCollector);

	CloseableSearchMapping boot();

	/**
	 * Runs only Phase 1 of bootstrap (mapping and index model creation),
	 * skipping Phase 2 (backend and index manager startup).
	 * <p>
	 * The returned partial mapping provides access to indexed entity metadata
	 * (including index field descriptors).
	 *
	 * @return A partial mapping that must be {@link StandalonePojoPartialMapping#close() closed}
	 * to release Phase 1 resources.
	 */
	@Incubating
	StandalonePojoPartialMapping bootPartial();

}
