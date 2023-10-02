/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public interface PojoMappingConfigurationContext {

	/**
	 * @param sourceType A source type, typically a container type such as Collection.
	 * @param extractorPath A container extractor path, possibly just {@link ContainerExtractorPath#defaultExtractors()}.
	 * @return The type of values extracted from the given source type with the given container extractor path,
	 * or {@link Optional#empty()} if the path cannot be applied.
	 */
	Optional<PojoTypeModel<?>> extractedValueType(PojoTypeModel<?> sourceType,
			ContainerExtractorPath extractorPath);

}
