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

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;

/**
 * Meta-annotation for annotations that bind a bridge to a property.
 * <p>
 * Whenever an annotation meta-annotated with {@link TypeBinding}
 * is found on a type in the domain model,
 * the corresponding binder will be applied to this type.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeBinding {

	/**
	 * @return A reference to the type binder to use.
	 * @see TypeBinderRef
	 */
	TypeBinderRef binder();

}
