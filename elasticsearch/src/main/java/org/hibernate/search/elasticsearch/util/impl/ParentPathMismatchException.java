/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;


/**
 * @author Yoann Rodiere
 */
public class ParentPathMismatchException extends Exception {

	private final String expectedParentPath;
	private final String mismatchingPath;

	public ParentPathMismatchException(String expectedParentPath, String mismatchingPath) {
		this.expectedParentPath = expectedParentPath;
		this.mismatchingPath = mismatchingPath;
	}

	@Override
	public String getMessage() {
		return "The path '" + mismatchingPath + "' is not contained within '" + expectedParentPath + "'" ;
	}

	public String getExpectedParentPath() {
		return expectedParentPath;
	}

	public String getMismatchingPath() {
		return mismatchingPath;
	}

}
