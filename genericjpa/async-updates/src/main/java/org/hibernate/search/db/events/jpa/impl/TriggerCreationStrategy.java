/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.jpa.impl;

import org.hibernate.search.exception.SearchException;

/**
 * Represents the possible strategies to be used for Trigger generation in the async backend
 *
 * @author Martin Braun
 */
public enum TriggerCreationStrategy {
	/**
	 * tries to create the triggers and update-tables, if triggers with the same name are already present,
	 * they are assumed to be the right triggers and we ignore possible Database Exceptions.
	 * <br>
	 * This is probably the best option for production usage without losing automatic SQL generation
	 */
	CREATE,
	/**
	 * behaves like {@link TriggerCreationStrategy#CREATE}, but drops the triggers and update-tables on shutdown
	 */
	CREATE_DROP,
	/**
	 * first drops the triggers and update-tables from the database (if there are any) and recreates everything
	 */
	DROP_CREATE,
	/**
	 * assumes the tables and triggers to already be in place and does nothing
	 */
	NONE;

	public static TriggerCreationStrategy fromString(String val) {
		switch ( val ) {
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE_DROP:
				return CREATE_DROP;
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE:
				return CREATE;
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_NONE:
				return NONE;
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE:
				return DROP_CREATE;
			default:
				throw new SearchException( "unrecognized Trigger creation strategy: " + val );
		}
	}

}
