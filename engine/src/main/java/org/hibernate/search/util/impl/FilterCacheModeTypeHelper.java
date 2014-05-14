/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.exception.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class FilterCacheModeTypeHelper {

	private FilterCacheModeTypeHelper() { }

	public static boolean cacheInstance(FilterCacheModeType type) {
		switch ( type ) {
			case NONE:
				return false;
			case INSTANCE_AND_DOCIDSETRESULTS:
				return true;
			case INSTANCE_ONLY:
				return true;
			default:
				throw new AssertionFailure("Unknown FilterCacheModeType:" + type);
		}
	}

	public static boolean cacheResults(FilterCacheModeType type) {
		switch ( type ) {
			case NONE:
				return false;
			case INSTANCE_AND_DOCIDSETRESULTS:
				return true;
			case INSTANCE_ONLY:
				return false;
			default:
				throw new AssertionFailure("Unknown FilterCacheModeType:" + type);
		}
	}
}
