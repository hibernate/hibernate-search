/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Objects whose identifier is provided externally and not part of the object state
 * should be marked with this annotation.
 * This is only meaningful when Hibernate Search is used to index Infinispan objects,
 * and should not be used to index JPA/Hibernate entities stored via Hibernate.
 * @deprecated with no replacement: this annotation will be removed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Deprecated
public @interface ProvidedId {

	String defaultFieldName = "providedId";

	String name() default defaultFieldName;

	FieldBridge bridge() default @FieldBridge(impl = org.hibernate.search.bridge.builtin.StringBridge.class);

}
