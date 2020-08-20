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
 * This annotation will be ignored as the Field Cache in Apache Lucene
 * feature does no longer exist.
 *
 * @deprecated Remove the annotation. No alternative replacement necessary.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.TYPE })
@Documented
@Deprecated
public @interface CacheFromIndex {

	/**
	 * @return Returns a {@code FieldCache} enum type indicating what kind of caching we should use for index stored metadata. Defaults to {@code FieldCache.CLASS}.
	 */
	FieldCacheType[] value() default { FieldCacheType.CLASS };

}
