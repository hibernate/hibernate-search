/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;

/**
 * Meta-annotation for annotations that bind a marker to a property.
 * <p>
 * Whenever an annotation meta-annotated with {@link MarkerBinding}
 * is found on a property in the domain model,
 * the corresponding binder will be applied to this property.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkerBinding {

	/**
	 * @return A reference to the marker binder to use.
	 * @see MarkerBinderRef
	 */
	MarkerBinderRef binder();

}
