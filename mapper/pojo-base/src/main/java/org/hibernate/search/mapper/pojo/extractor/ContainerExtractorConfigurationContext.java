/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor;

import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A context to assign names to container extractor implementations.
 *
 * @see ContainerExtractor
 * @see ContainerExtractorPath#explicitExtractor(String)
 * @see ContainerExtractorPath#explicitExtractors(List)
 */
@Incubating
@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public interface ContainerExtractorConfigurationContext {

	void define(String extractorName, Class<? extends ContainerExtractor> extractorClass);

	<C extends ContainerExtractor> void define(String extractorName, Class<C> extractorClass,
			BeanReference<? extends C> reference);

}
