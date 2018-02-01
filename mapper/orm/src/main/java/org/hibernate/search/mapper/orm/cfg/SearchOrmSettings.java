/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class SearchOrmSettings {

	public static final String PREFIX = "hibernate.search.";

	/**
	 * Enable listeners auto registration in Hibernate Annotations and EntityManager. Default to true.
	 */
	public static final String AUTOREGISTER_LISTENERS = PREFIX + Radicals.AUTOREGISTER_LISTENERS;

	/**
	 * Defines the indexing strategy, default <code>event</code>
	 * Other options <code>manual</code>
	 */
	public static final String INDEXING_STRATEGY = PREFIX + Radicals.INDEXING_STRATEGY;

	/**
	 * When enabled re-indexing of an entity is skipped if the updates affect only non-indexed fields.
	 * Enabled by default as it should be safe and should improve performance, disable it to force updates
	 * skipping value checks.
	 * Affect semantics of entity updates only.
	 */
	public static final String ENABLE_DIRTY_CHECK = PREFIX + Radicals.ENABLE_DIRTY_CHECK;

	/**
	 * Provide a programmatic mapping model to Hibernate Search configuration
	 * Accepts a {@link org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor}
	 * instance or the fully qualified class name of a HibernateOrmSearchMappingContributor subclass.
	 * Such a subclass must have a no-arg constructor.
	 */
	public static final String MAPPING_CONTRIBUTOR = PREFIX + Radicals.MAPPING_CONTRIBUTOR;

	public static class Radicals {
		public static final String AUTOREGISTER_LISTENERS = "autoregister_listeners";
		public static final String INDEXING_STRATEGY = "indexing_strategy";
		public static final String ENABLE_DIRTY_CHECK = "enable_dirty_check";
		public static final String MAPPING_CONTRIBUTOR = "mapping_contributor";

		private Radicals() {
		}
	}

	private SearchOrmSettings() {
	}

}
