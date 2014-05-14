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
 * While extracting values from the index might be faster than extracting
 * them from a Database, it might still involve costly IO operations.
 * Depending on the type of performed queries, it's often needed to extract
 * at least the entity type. Caches which aren't needed won't be used, but used
 * caches might take a significant amount of memory.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.TYPE })
@Documented
public @interface CacheFromIndex {

	/**
	 * @return Returns a {@code FieldCache} enum type indicating what kind of caching we should use for index stored metadata. Defaults to {@code FieldCache.CLASS}.
	 */
	FieldCacheType[] value() default { FieldCacheType.CLASS };

}
