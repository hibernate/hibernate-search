/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;

/**
 * Throwable-related utils.
 *
 */
public final class Throwables {

	private Throwables() {
	}

	public static RuntimeException toRuntimeException(Throwable throwable) {
		if ( throwable instanceof RuntimeException ) {
			return (RuntimeException) throwable;
		}
		else if ( throwable instanceof Error ) {
			// Do not wrap errors: it would be "unreasonable" according to the Error javadoc
			throw (Error) throwable;
		}
		else if ( throwable == null ) {
			throw new AssertionFailure( "Null throwable" );
		}
		else {
			return new SearchException( throwable.getMessage(), throwable );
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
			throw new AssertionFailure( "Null throwable" );
		}
		else {
			throw new AssertionFailure( "Unexpected throwable type", throwable );
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

	public static String getFirstNonNullMessage(Throwable t) {
		Throwable cause = t.getCause();
		while ( t.getMessage() == null && cause != null ) {
			t = cause;
			cause = t.getCause();
		}
		return t.getMessage();
	}

	public static String safeToString(Throwable throwableBeingHandled, Object object) {
		if ( object == null ) {
			return "null";
		}
		if ( object instanceof Object[] ) {
			return safeArrayToString( throwableBeingHandled, (Object[]) object );
		}
		try {
			return object.toString();
		}
		catch (Throwable t) {
			throwableBeingHandled.addSuppressed( t );
			return "<" + object.getClass().getSimpleName() + "#toString() threw " + t.getClass().getSimpleName() + ">";
		}
	}

	private static String safeArrayToString(Throwable throwableBeingHandled, Object[] array) {
		if ( array == null ) {
			return "null";
		}
		StringBuilder b = new StringBuilder();
		b.append( '[' );
		for ( int i = 0; i < array.length; i++ ) {
			if ( i > 0 ) {
				b.append( ", " );
			}
			b.append( safeToString( throwableBeingHandled, array[i] ) );
		}
		b.append( ']' );
		return b.toString();
	}

}
