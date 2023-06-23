/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public final class PojoMappingConfigurationContextImpl implements PojoMappingConfigurationContext {

	private final ContainerExtractorBinder extractorBinder;

	public PojoMappingConfigurationContextImpl(ContainerExtractorBinder extractorBinder) {
		this.extractorBinder = extractorBinder;
	}

	@Override
	public Optional<PojoTypeModel<?>> extractedValueType(PojoTypeModel<?> sourceType,
			ContainerExtractorPath extractorPath) {
		return extractorBinder.tryBindPath( sourceType, extractorPath )
				.map( BoundContainerExtractorPath::getExtractedType );
	}

}
