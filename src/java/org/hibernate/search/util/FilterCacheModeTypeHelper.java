package org.hibernate.search.util;

import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class FilterCacheModeTypeHelper {
	private FilterCacheModeTypeHelper() {}

	public static boolean cacheInstance(FilterCacheModeType type) {
		switch ( type ) {
			case NONE:
				return false;
			case INSTANCE_AND_DOCIDSETRESULTS:
				return true;
			case INSTANCE_ONLY:
				return true;
			default:
				throw new AssertionFailure("Unknwn FilterCacheModeType:" + type);
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
				throw new AssertionFailure("Unknwn FilterCacheModeType:" + type);
		}
	}
}
