/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.hibernate.search.exception.AssertionFailure;

/**
 * Throwable-related utils.
 *
 * @author Yoann Rodiere
 */
public final class Throwables {

	private Throwables() {
	}

	public static RuntimeException expectRuntimeException(Throwable throwable) {
		if ( throwable instanceof RuntimeException ) {
			return (RuntimeException) throwable;
		}
		else if ( throwable instanceof Error ) {
			// Do not wrap errors: it would be "unreasonable" according to the Error javadoc
			throw (Error) throwable;
		}
		else if ( throwable == null ) {
			throw new AssertionFailure( "Null throwable - there is probably a bug" );
		}
		else {
			throw new AssertionFailure( "Unexpected throwable type - there is probably a bug", throwable );
		}
	}

	public static Exception expectException(Throwable throwable) {
		if ( throwable instanceof Exception ) {
			return (Exception) throwable;
		}
		else if ( throwable instanceof Error ) {
			// Do not wrap errors: it would be "unreasonable" according to the Error javadoc
			throw (Error) throwable;
		}
		else if ( throwable == null ) {
			throw new AssertionFailure( "Null throwable - there is probably a bug" );
		}
		else {
			throw new AssertionFailure( "Unexpected throwable type - there is probably a bug", throwable );
		}
	}

	public static <T extends Throwable> T combine(T throwable, T otherThrowable) {
		T toThrow = throwable;
		if ( otherThrowable != null ) {
			if ( toThrow != null ) {
				toThrow.addSuppressed( otherThrowable );
			}
			else {
				toThrow = otherThrowable;
			}
		}
		return toThrow;
	}

}
