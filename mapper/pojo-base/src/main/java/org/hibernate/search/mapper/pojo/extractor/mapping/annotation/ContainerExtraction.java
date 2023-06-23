/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A definition of container value extractors to be applied to a property,
 * allowing reference to a specific value of a container property.
 * <p>
 * For instance, on a property of type {@code Map<EntityA, EntityB>},
 * {@code @ContainerExtraction(@ContainerExtractor(BuiltinContainerExtractors.MAP_KEY))}
 * would point to the map keys (of type {@code EntityA}),
 * while {@code @ContainerExtraction(@ContainerExtractor(BuiltinContainerExtractors.MAP_VALUE))}
 * would point to the map values (of type {@code EntityB}).
 * <p>
 * By default, if no attributes are set on this annotation,
 * Hibernate Search will try to apply a set of extractors for common types
 * ({@link Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...)
 * and use the first one that works.
 * To prevent Hibernate Search from applying any extractor,
 * use {@code @ContainerExtraction(extract = ContainerExtract.NO)}
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerExtraction {

	/**
	 * @return Whether values are extracted from containers or not.
	 * @see ContainerExtract
	 */
	ContainerExtract extract() default ContainerExtract.DEFAULT;

	/**
	 * @return An array of container value extractor names.
	 * Setting this together with {@code extract = } {@link ContainerExtract#NO} will trigger an exception.
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	String[] value() default { };

}
