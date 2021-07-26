/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination;


public final class CoordinationStrategyNames {

	private CoordinationStrategyNames() {
	}

	/**
	 * No coordination: only one application node exists,
	 * or multiple application nodes exist,
	 * but they do not coordinate with each other,
	 * performing indexing and search requests independently.
	 * <p>
	 * <strong>Warning:</strong> If you want more than one node, even with this strategy,
	 * you must use a backend that can share its data across multiple nodes;
	 * at the moment the Lucene backend cannot, and only the Elasticsearch backend can.
	 * <p>
	 * Compared to actual coordination strategies,
	 * this strategy has the advantage of enabling synchronous automatic indexing,
	 * though performance may take a hit with the most strict settings
	 * (see {@link org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames}).
	 * <p>
	 * However, in rare cases where the same document involving {@code @IndexedEmbedded} is changed by two concurrent transactions,
	 * or where backend indexing requests fail due to I/O errors,
	 * automatic indexing with this strategy may lead to out-of sync indexes.
	 * See <a href="https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#limitations-parallel-embedded-update">this section of the reference documentation</a>
	 * for more information.
	 */
	public static final String NONE = "none";

	/**
	 * Database polling: one or multiple application nodes exist,
	 * and they coordinate with each other by pushing data to additional tables in the database
	 * and polling these tables.
	 * <p>
	 * Compared to the {@link #NONE} strategy,
	 * this strategy has the advantage of being immune to
	 * <a href="https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#limitations-parallel-embedded-update">limitations that could lead to out-of-sync indexes</a>,
	 * at the cost of requiring all automatic indexing to be asynchronous.
	 * <p>
	 * Compared to other multi-node strategies (to be implemented),
	 * this strategy has the advantage of not requiring any additional infrastructure,
	 * since it relies exclusively on the database.
	 */
	public static final String DATABASE_POLLING = "database-polling";

}
