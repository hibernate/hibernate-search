/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

/**
 * @author Yoann Rodiere
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexedEmbedded {

	String prefix() default "";

	int maxDepth() default -1;

	String[] includePaths() default {};

	ObjectFieldStorage storage() default ObjectFieldStorage.DEFAULT;

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the definition of an indexed-embedded for container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<TypeA, TypeB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows referencing map keys instead of map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	// TODO HSEARCH-3071 includeEmbeddedObjectId
	// TODO HSEARCH-3072 targetElement

}
