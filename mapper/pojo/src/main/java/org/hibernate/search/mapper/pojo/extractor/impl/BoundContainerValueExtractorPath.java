/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.mapper.pojo.extractor.spi.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;

/**
 * A {@link ContainerValueExtractorPath} bound to a given source type.
 * <p>
 * Instances are returned by the {@code bind}/{@code tryBind} methods of {@link ContainerValueExtractorBinder}.
 * <p>
 * The {@link #getExtractorPath() extractor path} is guaranteed to be explicit
 * ({@link ContainerValueExtractorPath#isDefault()} returns {@code false}),
 * and to be valid when applied to the source type.
 * <p>
 * The extractor path may be empty, in which case the source type is equal to the extracted type.
 *
 * @param <C> The container type
 * @param <V> The extracted value type
 */
public class BoundContainerValueExtractorPath<C, V> {
	public static <V> BoundContainerValueExtractorPath<V, V> noExtractors(PojoGenericTypeModel<V> sourceType) {
		return new BoundContainerValueExtractorPath<>(
				sourceType, ContainerValueExtractorPath.noExtractors(), sourceType
		);
	}

	private final PojoGenericTypeModel<C> sourceType;
	private final ContainerValueExtractorPath extractorPath;
	private final PojoGenericTypeModel<V> extractedType;

	BoundContainerValueExtractorPath(PojoGenericTypeModel<C> sourceType, ContainerValueExtractorPath extractorPath,
			PojoGenericTypeModel<V> extractedType) {
		this.sourceType = sourceType;
		this.extractorPath = extractorPath;
		this.extractedType = extractedType;
	}

	public PojoGenericTypeModel<C> getSourceType() {
		return sourceType;
	}

	public ContainerValueExtractorPath getExtractorPath() {
		return extractorPath;
	}

	public PojoGenericTypeModel<V> getExtractedType() {
		return extractedType;
	}
}
