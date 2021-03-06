/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing;


import org.hibernate.search.mapper.orm.session.SearchSession;

public final class AutomaticIndexingStrategyNames {

	private AutomaticIndexingStrategyNames() {
	}

	/**
	 * No automatic indexing is performed:
	 * indexing will only happen when explicitly requested through APIs
	 * such as {@link SearchSession#indexingPlan()}.
	 */
	public static final String NONE = "none";

	/**
	 * Indexing is triggered automatically when entities are modified in the Hibernate ORM session:
	 * entity insertion, update etc.
	 */
	public static final String SESSION = "session";

	/**
	 * When entities are modified in the Hibernate ORM session, indexing events are stored in the Outbox table.
	 * At the same time a background thread uses the table to update the indexes.
	 */
	public static final String OUTBOX_POLLING = "outbox-polling";

}
