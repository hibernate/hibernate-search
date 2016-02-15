/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.jpa.impl;

/**
 * Created by Martin on 10.11.2015.
 */
public final class AsyncUpdateConstants {

	private AsyncUpdateConstants() {
		// can't touch this!
	}

	public static final String BATCH_SIZE_FOR_UPDATE_QUERIES_KEY = "hibernate.search.trigger.batchSizeForUpdateQueries";
	public static final String BATCH_SIZE_FOR_UPDATE_QUERIES_DEFAULT_VALUE = "20";

	public static final String TRIGGER_SOURCE_KEY = "hibernate.search.trigger.source";

	public static final String UPDATE_DELAY_KEY = "hibernate.search.trigger.updateDelay";
	public static final String UPDATE_DELAY_DEFAULT_VALUE = "500";
	public static final String BATCH_SIZE_FOR_UPDATES_KEY = "hibernate.search.trigger.batchSizeForUpdates";
	public static final String BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE = "5";

	public static final String TRIGGER_CREATION_STRATEGY_KEY = "hibernate.search.trigger.createstrategy";
	public static final String TRIGGER_CREATION_STRATEGY_CREATE = "create";
	public static final String TRIGGER_CREATION_STRATEGY_DROP_CREATE = "drop-create";
	public static final String TRIGGER_CREATION_STRATEGY_DONT_CREATE = "dont-create";
	public static final String TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE = TRIGGER_CREATION_STRATEGY_CREATE;

}
