/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyValue {

	String propertyName();

	/**
	 * @return An array of reference to container value extractor implementation classes,
	 * which will be applied to the property when getting the value.
	 * By default, Hibernate Search will try to apply a set of extractors for common types
	 * ({@link Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...).
	 * To prevent Hibernate Search from applying any extractor, set this attribute to an empty array (<code>{}</code>).
	 */
	ContainerValueExtractorBeanReference[] extractors()
			default @ContainerValueExtractorBeanReference(type = ContainerValueExtractorBeanReference.DefaultExtractors.class);

}
