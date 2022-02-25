/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
