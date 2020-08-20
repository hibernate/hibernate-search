/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

/**
 * Defines the index structure mode used for spatial information
 *
 * @author Hardy Ferentschik
 */
public enum SpatialMode {
	/**
	 * Range mode : double range query + distance filter
	 */
	RANGE,
	/**
	 * Hash mode : Spatial hash index query + distance filer
	 */
	HASH
}
