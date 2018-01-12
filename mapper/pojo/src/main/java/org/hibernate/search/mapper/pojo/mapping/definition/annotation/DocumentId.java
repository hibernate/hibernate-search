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

/**
 * @author Yoann Rodiere
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
// TODO repeatable
public @interface DocumentId {

	/**
	 * @return A reference to the identifier bridge to use for document IDs.
	 * Cannot be used in the same {@code @DocumentId} annotation as {@link #identifierBridgeBuilder()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	IdentifierBridgeBeanReference identifierBridge() default @IdentifierBridgeBeanReference;

	/**
	 * @return A reference to the builder to use to builder an identifier bridge for document IDs.
	 * Cannot be used in the same {@code @DocumentId} annotation as {@link #identifierBridge()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	IdentifierBridgeBuilderBeanReference identifierBridgeBuilder() default @IdentifierBridgeBuilderBeanReference;

}
