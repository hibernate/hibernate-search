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

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;

/**
 * Maps a property to the identifier of documents in the index.
 * <p>
 * This annotation is only taken into account on {@link Indexed} types.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentId {

	/**
	 * @return A reference to the identifier bridge to use for document IDs.
	 * Must not be set if {@link #identifierBinder()} is set.
	 * @see IdentifierBridgeRef
	 */
	IdentifierBridgeRef identifierBridge() default @IdentifierBridgeRef;

	/**
	 * @return A reference to the identifier binder to use for document IDs.
	 * Must not be set if {@link #identifierBridge()} is set.
	 * @see IdentifierBinderRef
	 */
	IdentifierBinderRef identifierBinder() default @IdentifierBinderRef;

}
