/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;


/**
 * @author Yoann Rodiere
 */
public final class MassIndexingJobParameters {

	private MassIndexingJobParameters() {
		// Private constructor, do not use
	}

	public static final String ENTITY_MANAGER_FACTORY_SCOPE = "entityManagerFactoryScope";

	public static final String ENTITY_MANAGER_FACTORY_REFERENCE = "entityManagerFactoryReference";

	public static final String ROOT_ENTITIES = "rootEntities";

	public static final String MAX_THREADS = "maxThreads";

	public static final String FETCH_SIZE = "fetchSize";

	public static final String CACHEABLE = "cacheable";

	public static final String OPTIMIZE_AT_END = "optimizeAtEnd";

	public static final String OPTIMIZE_AFTER_PURGE = "optimizeAfterPurge";

	public static final String PURGE_AT_START = "purgeAtStart";

	public static final String ROWS_PER_PARTITION = "rowsPerPartition";

	public static final String ITEM_COUNT = "itemCount";

	public static final String HQL = "hql";

	public static final String CRITERIA = "criteria";

	public static final String MAX_RESULTS = "maxResults";
}
