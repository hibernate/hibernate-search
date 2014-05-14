/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.annotations;

/**
 * How should we interact with Lucene's {@code FieldCache}.
 * {@code FieldCache}s can provide a good performance boost
 * but will keep soft references to large arrays of values in memory.
 * This effectively caches stored values from the index to reduce seek times.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see org.apache.lucene.search.FieldCache
 */
public enum FieldCacheType {

	/**
	 * Disable all caching options. (ie CLASS, ID)
	 * CacheFromIndex(NOTHING) or CacheFromIndex(value=FieldCacheType[]{}) are equivalent.
	 */
	NOTHING,

	/**
	 * Cache the entity type. This is a good tradeoff in most cases as
	 * it enables some optimizations; Depending on the query the type might not be
	 * needed, in which case the FieldCache won't be used.
	 */
	CLASS,

	/**
	 * Attempts to the object identifier (@DocumentId).
	 * Not all identifier types are supported.
	 */
	ID
}
