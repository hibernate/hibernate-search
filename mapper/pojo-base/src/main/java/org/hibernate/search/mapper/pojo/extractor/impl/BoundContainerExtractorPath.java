/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * A {@link ContainerExtractorPath} bound to a given source type.
 * <p>
 * Instances are returned by the {@code bind}/{@code tryBind} methods of {@link ContainerExtractorBinder}.
 * <p>
 * The {@link #getExtractorPath() extractor path} is guaranteed to be explicit
 * ({@link ContainerExtractorPath#isDefault()} returns {@code false}),
 * and to be valid when applied to the source type.
 * <p>
 * The extractor path may be empty, in which case the source type is equal to the extracted type.
 *
 * @param <C> The container type
 * @param <V> The extracted value type
 */
public class BoundContainerExtractorPath<C, V> {
	public static <V> BoundContainerExtractorPath<V, V> noExtractors(PojoTypeModel<V> sourceType) {
		return new BoundContainerExtractorPath<>(
				ContainerExtractorPath.noExtractors(), sourceType
		);
	}

	private final ContainerExtractorPath extractorPath;
	private final PojoTypeModel<V> extractedType;

	BoundContainerExtractorPath(ContainerExtractorPath extractorPath, PojoTypeModel<V> extractedType) {
		this.extractorPath = extractorPath;
		this.extractedType = extractedType;
	}

	public ContainerExtractorPath getExtractorPath() {
		return extractorPath;
	}

	public PojoTypeModel<V> getExtractedType() {
		return extractedType;
	}
}
