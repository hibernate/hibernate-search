/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db;

import org.hibernate.search.genericjpa.exception.AssertionFailure;

/**
 * Contains constants that describe the different Database-Events that are relevant to the index
 *
 * @author Martin Braun
 */
public final class EventType {

	public static final int DELETE = -1;
	public static final int UPDATE = -2;
	public static final int INSERT = -3;
	private static final int[] VALUES = {DELETE, UPDATE, INSERT};

	private EventType() {
		throw new AssertionFailure( "can't touch this!" );
	}

	public static String toString(int eventType) {
		switch ( eventType ) {
			case DELETE:
				return "DELETE";
			case UPDATE:
				return "UPDATE";
			case INSERT:
				return "INSERT";
		}
		throw new IllegalArgumentException( "unrecognized eventType" );
	}

	public static int[] values() {
		return VALUES;
	}

}
