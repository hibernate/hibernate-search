/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.annotations;

/**
 * This will be ignored as the Field Cache in Apache Lucene
 * feature does no longer exist.
 *
 * @deprecated Remove the annotation. No alternative replacement necessary.
 */
@Deprecated
public enum FieldCacheType {

	/**
	 * Disable all caching options. (ie CLASS, ID)
	 * CacheFromIndex(NOTHING) or CacheFromIndex(value=FieldCacheType[]{}) are equivalent.
	 */
	NOTHING,

	/**
	 * Same effect as using {@literal NOTHING} as this feature was removed.
	 */
	CLASS,

	/**
	 * Same effect as using {@literal NOTHING} as this feature was removed.
	 */
	ID
}
