/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * A superinterface for contexts passed to mapping annotation processors.
 */
public interface MappingAnnotationProcessorContext {

	/**
	 * Convert an {@link ObjectPath} annotation to a {@link PojoModelPathValueNode}.
	 * @param objectPath The annotation to convert.
	 * @return The corresponding path, or an empty optional if the path was empty.
	 */
	Optional<PojoModelPathValueNode> toPojoModelPathValueNode(ObjectPath objectPath);

	/**
	 * Convert a {@link ContainerExtraction} annotation to a {@link ContainerExtractorPath}.
	 * @param extraction The annotation to convert.
	 * @return The corresponding path.
	 */
	ContainerExtractorPath toContainerExtractorPath(ContainerExtraction extraction);

	/**
	 * Convert attributes of a bean-reference annotation,
	 * such as {@link org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef},
	 * to an actual {@link BeanReference}.
	 * <p>
	 * Example of use:
	 * <pre>{@code
	 *     Optional<BeanReference<? extends ValueBridge>> valueBridgeRef = toBeanReference(
	 *             ValueBridge.class,
	 *             ValueBridgeRef.UndefinedBridgeImplementationType.class,
	 *             myValueBridgeRefInstance.type(),
	 *             myValueBridgeRefInstance.name()
	 *     );
	 * }</pre>
	 *
	 * @param expectedType The supertype of all types that can be referenced.
	 * @param undefinedTypeMarker A marker type to detect that the {@code type} parameter has its default value (undefined).
	 * @param type The bean type.
	 * @param name The bean name.
	 * @param <T> The bean type.
	 * @return The corresponding bean reference, or an empty optional if neither the type nor the name is provided.
	 */
	<T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name);

}
