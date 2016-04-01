/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Contains constants that describe the different Database-Events that are relevant to the index
 *
 * @author Martin Braun
 * @hsearch.experimental
 */
public enum EventType {
	DELETE( -1 ),
	UPDATE( -2 ),
	INSERT( -3 );

	private final int identifier;

	private static final Log log = LoggerFactory.make();

	EventType(int identifier) {
		this.identifier = identifier;
	}

	public static EventType valueOf(Integer val) {
		switch ( val ) {
			case -1:
				return DELETE;
			case -2:
				return UPDATE;
			case -3:
				return INSERT;
			default:
				throw new AssertionFailure( "unknown EventType identifier: " + val );
		}
	}

	public int getIdentifier() {
		return identifier;
	}
}
