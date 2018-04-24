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
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

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
	 * @return An array of reference to container value extractor implementation classes,
	 * which will be applied to the source value before applying this bridge.
	 * By default, Hibernate Search will try to apply a set of extractors for common types
	 * ({@link java.lang.Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...).
	 * To prevent Hibernate Search from applying any extractor, set this attribute to an empty array (<code>{}</code>).
	 */
	ContainerValueExtractorBeanReference[] extractors()
			default @ContainerValueExtractorBeanReference( type = DefaultExtractors.class );

	/**
	 * Class used as a marker for the default value of the {@link #extractors()} attribute.
	 */
	abstract class DefaultExtractors implements ContainerValueExtractor<Object, Object> {
		private DefaultExtractors() {
		}
	}

	// TODO includeEmbeddedObjectId?
	// TODO targetElement?
	// TODO indexNullAs? => Maybe we should rather use "missing" queries?

}
