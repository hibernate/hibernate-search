/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractMappingAnnotationProcessorContext
		implements MappingAnnotationProcessorContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Optional<PojoModelPathValueNode> toPojoModelPathValueNode(ObjectPath objectPath) {
		return MappingAnnotationProcessorUtils.toPojoModelPathValueNode( objectPath );
	}

	@Override
	public ContainerExtractorPath toContainerExtractorPath(ContainerExtraction extraction) {
		return MappingAnnotationProcessorUtils.toContainerExtractorPath( extraction );
	}

	@Override
	public <T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name) {
		return MappingAnnotationProcessorUtils.toBeanReference( expectedType, undefinedTypeMarker, type, name );
	}

}
