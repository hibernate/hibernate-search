/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi.MappingAnnotationProcessorUtils;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

public abstract class AbstractMappingAnnotationProcessorContext
		implements MappingAnnotationProcessorContext {

	protected final AnnotationHelper annotationHelper;

	protected AbstractMappingAnnotationProcessorContext(AnnotationHelper annotationHelper) {
		this.annotationHelper = annotationHelper;
	}

	@Override
	public Optional<PojoModelPathValueNode> toPojoModelPathValueNode(ObjectPath objectPath) {
		try {
			return MappingAnnotationProcessorUtils.toPojoModelPathValueNode( objectPath );
		}
		catch (IllegalArgumentException e) {
			throw MappingLog.INSTANCE.invalidObjectPath( objectPath, e.getMessage(), e );
		}
	}

	@Override
	public ContainerExtractorPath toContainerExtractorPath(ContainerExtraction extraction) {
		return MappingAnnotationProcessorUtils.toContainerExtractorPath( extraction );
	}

	@Override
	public <T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name, BeanRetrieval retrieval) {
		return MappingAnnotationProcessorUtils.toBeanReference( expectedType, undefinedTypeMarker, type, name, retrieval );
	}

	@Override
	public <T> T toNullIfDefault(T value, T defaultValue) {
		return Objects.equals( value, defaultValue ) ? null : value;
	}
}
