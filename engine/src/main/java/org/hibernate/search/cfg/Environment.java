/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.Map;

import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.impl.DefaultIndexManagerFactory;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Environment {

	/**
	 * Property name to set the default object lookup method during object initialization. As value lower or upper
	 * cased enum names are allowed.
	 *
	 * @see org.hibernate.search.query.ObjectLookupMethod
	 */
	public static final String OBJECT_LOOKUP_METHOD = "hibernate.search.query.object_lookup_method";

	/**
	 * Property name to set the default database retrieval method during object initialization. As value lower or upper
	 * cased enum names are allowed.
	 *
	 * @see org.hibernate.search.query.DatabaseRetrievalMethod
	 */
	public static final String DATABASE_RETRIEVAL_METHOD = "hibernate.search.query.database_retrieval_method";

	/**
	 * Enable listeners auto registration in Hibernate Annotations and EntityManager. Default to true.
	 */
	public static final String AUTOREGISTER_LISTENERS = "hibernate.search.autoregister_listeners";

	/**
	 * Defines the indexing strategy, default <code>event</code>
	 * Other options <code>manual</code>
	 */
	public static final String INDEXING_STRATEGY = "hibernate.search.indexing_strategy";

	/**
	 * Default Lucene analyser
	 */
	public static final String ANALYZER_CLASS = "hibernate.search.analyzer";

	/**
	 * Default Lucene similarity
	 */
	public static final String SIMILARITY_CLASS = "hibernate.search.similarity";


	public static final String WORKER_SCOPE = "hibernate.search.worker.scope";

	/**
	 * When set to {@code true} the indexing operations will be passed to the indexing backend within
	 * the transaction. When {@code false} the indexing backends are invoked as a post-transaction hook.
	 * Setting this to {@code true} is currently only supported when you use the JMS backends exclusively.
	 * Defaults to {@code false}.
	 */
	public static final String WORKER_ENLIST_IN_TRANSACTION = "hibernate.search.worker.enlist_in_transaction";

	public static final String WORKER_PREFIX = "worker.";
	public static final String WORKER_BACKEND = WORKER_PREFIX + "backend";
	public static final String WORKER_EXECUTION = WORKER_PREFIX + "execution";

	/**
	 * Defines the maximum number of indexing operation batched per transaction
	 */
	public static final String QUEUEINGPROCESSOR_BATCHSIZE = "hibernate.search.batch_size";

	/**
	 * define the reader prefix
	 */
	public static final String READER_PREFIX = "reader";

	/**
	 * define the reader strategy used
	 */
	public static final String READER_STRATEGY = READER_PREFIX + "." + "strategy";

	/**
	 * filter caching strategy class (must have a no-arg constructor and implement FilterCachingStrategy)
	 */
	public static final String FILTER_CACHING_STRATEGY = "hibernate.search.filter.cache_strategy";

	/**
	 * number of docidresults cached in hard reference.
	 */
	public static final String CACHE_DOCIDRESULTS_SIZE = "hibernate.search.filter.cache_docidresults.size";

	/**
	 * When set to true a lock on the index will not be released until the
	 * SearchIntegrator (or SessionFactory) is closed.
	 * This improves performance in applying changes to the index, but no other application
	 * can access the index in write mode while Hibernate Search is running.
	 * This is an index-scoped property and defaults to false.
	 */
	public static final String EXCLUSIVE_INDEX_USE = "exclusive_index_use";

	/**
	 * Boolean setting, defaults to <code>true</code>.
	 * Unless it's disabled Hibernate Search will assume it knows all entities mapped
	 * to this index and will enable all optimizations which are considered safe
	 * in the known schema.
	 * For example it will delete indexed entities from the index using only the
	 * identifier if there is only one type in the index; when multiple types share
	 * the same index or this option is disabled it will need to delete entities
	 * using both the id and the classname.
	 */
	public static final String INDEX_METADATA_COMPLETE = "index_metadata_complete";

	/**
	 * Number of times we retry the logic looking for marker files in master's directory before
	 * giving up and raising an exception.
	 * This setting is the suffix of an index using FSSlaveDirectoryProvider
	 * <p>
	 * Note that we try again after 5 seconds.
	 * <p>
	 * Default to 0 (ie no retry).
	 */
	public static final String RETRY_MARKER_LOOKUP = "retry_marker_lookup";

	/**
	 * Define the similarity class name for a given index
	 */
	public static final String SIMILARITY_CLASS_PER_INDEX = "similarity";

	/**
	 * Provide a programmatic mapping model to Hibernate Search configuration
	 * Accepts a fully populated SearchMapping object or a fully qualified
	 * class name of a SearchMapping factory. Such a factory must have:
	 * - a no-arg constructor
	 * - a method returning SearchMapping and annotated with @Factory
	 */
	public static final String MODEL_MAPPING = "hibernate.search.model_mapping";

	/**
	 * Option for specifying an error handler used during processing of the Lucene updates. Supported value types are:
	 * <ul>
	 * <li>{@code String}: the fully qualified name of an {@link org.hibernate.search.exception.ErrorHandler} implementation</li>
	 * <li>{@code ErrorHandler}: an error handler instance</li>
	 * </ul>
	 * Default is to log errors.
	 */
	public static final String ERROR_HANDLER = "hibernate.search.error_handler";

	/**
	 * If set to {@code true} JMX beans get enabled. For all other values the beans do not.
	 * get enabled.
	 */
	public static final String JMX_ENABLED = "hibernate.search.jmx_enabled";

	/**
	 * If JMX bean deployment is enabled (see {@link #JMX_ENABLED}) the specified suffix will be appended to the registered
	 * MBean names. This allows for running multiple apps on the same JVM each which JMX enabled. If not specified only the default
	 * names are used.
	 */
	public static final String JMX_BEAN_SUFFIX = "hibernate.search.jmx_bean_suffix";

	/**
	 * If set to {@code true} the search statistic will be gathered.
	 */
	public static final String GENERATE_STATS = "hibernate.search.generate_statistics";

	/**
	 * The Lucene match version parameter. Highly recommended since Lucene 3.
	 */
	public static final String LUCENE_MATCH_VERSION = "hibernate.search.lucene_version";

	/**
	 * Parameter name used to configure the default null token
	 */
	public static final String DEFAULT_NULL_TOKEN = "hibernate.search.default_null_token";

	/**
	 * When enabled re-indexing of an entity is skipped if the updates affect only non-indexed fields.
	 * Enabled by default as it should be safe and should improve performance, disable it to force updates
	 * skipping value checks.
	 * Affect semantics of entity updates only.
	 */
	public static final String ENABLE_DIRTY_CHECK = "hibernate.search.enable_dirty_check";

	/**
	 * The lucene backend has a separate writing thread for each index, the work pushed to each thread
	 * is put in a queue which grows up to a maximum number of elements, which is configured by this
	 * setting and defaults to 1000.
	 * When the limit is reached work producers are blocked until some work has been processed.
	 */
	public static final String MAX_QUEUE_LENGTH = "max_queue_length";

	/**
	 * The lucene backend can operate in async mode, and can apply changes to the index at
	 * regular intervals, effectively collapsing incoming changesets in order to reduce the
	 * amount of commits. This property specifies the interval in ms that commits will be done.
	 * This property will be ignored unless aync indexing is enabled.
	 */
	public static final String INDEX_FLUSH_INTERVAL = "index_flush_interval";

	/**
	 * If nothing else is specified we use {@code Version.LUCENE_CURRENT} as the default Lucene version. This version
	 * parameter was introduced by Lucene to attempt providing backwards compatibility when upgrading Lucene versions
	 * and not wanting to rebuild the index from scratch. It's highly recommended to specify a version, so that you
	 * can upgrade Hibernate Search and control when to eventually upgrade the Lucene format.
	 */
	public static final org.apache.lucene.util.Version DEFAULT_LUCENE_MATCH_VERSION = org.apache.lucene.util.Version.LATEST;

	/**
	 * Used to specify an alternative IndexManager implementation for a specific index.
	 * This is an index scoped property, so it needs to be prefixed by default or the index name, for example:
	 * <ul>
	 * <li>hibernate.search.default.indexmanager</li>
	 * <li>hibernate.search.Books.indexmanager</li>
	 * </ul>
	 */
	public static final String INDEX_MANAGER_IMPL_NAME = "indexmanager";

	/**
	 * Name of the JMS message property containing the index name to which to apply remote work. <i>JMSXGroupID</i> is
	 * actually a standard JMS header field which is used for message grouping. See HSEARCH-1922.
	 */
	public static final String INDEX_NAME_JMS_PROPERTY = "JMSXGroupID";

	/**
	 * Option for setting the locking strategy to be used for locking Lucene directories. Supported values are:
	 * <ul>
	 * <li><code>simple</code></li>
	 * <li><code>native</code></li>
	 * <li><code>single</code></li>
	 * <li><code>none</code></li>
	 * <li>The fully-qualified name of a custom {@link org.hibernate.search.store.LockFactoryProvider} implementation.</li>
	 * </ul>
	 * Can be given globally or for specific indexes:
	 * <ul>
	 * <li><code>hibernate.search.default.locking_strategy=simple</code></li>
	 * <li><code>hibernate.search.Books.locking_strategy=org.custom.components.MyLockingFactory</code></li>
	 * </ul>
	 */
	public static final String LOCKING_STRATEGY = "locking_strategy";

	/**
	 * Option for setting the base directory for storing Lucene indexes when working with file-system based directories.
	 * To be given globally:
	 * <p>
	 * <code>hibernate.search.default.indexBase=/var/lucene/indexes</code>
	 */
	public static final String INDEX_BASE_PROP_NAME = "indexBase";

	/**
	 * Option for specifying an index name for specific entities. To be given per entity:
	 * <p>
	 * <code>hibernate.search.com.example.Furniture.indexName=FurnitureIndex</code>
	 */
	public static final String INDEX_NAME_PROP_NAME = "indexName";

	/**
	 * Option for allowing or disallowing index uninverting when sorting on fields not covered by doc value fields. If
	 * disallowed and and uncovered sort is detected, an exception will be raised, otherwise only a warning will be
	 * logged.
	 * <p>
	 * Allowed values are "true" and "false". Defaults to "true".
	 *
	 * @see org.hibernate.search.annotations.SortableField
	 */
	public static final String INDEX_UNINVERTING_ALLOWED = "hibernate.search.index_uninverting_allowed";

	public static final Map<Class<? extends Service>, String> DEFAULT_SERVICES_MAP;
	// TODO for now we hard code the default services. This could/should be made configurable (HF)
	static
	{
		DEFAULT_SERVICES_MAP = CollectionHelper.newHashMap( 1 );
		DEFAULT_SERVICES_MAP.put( IndexManagerFactory.class, DefaultIndexManagerFactory.class.getName() );
	}

	private Environment() {
	}

}
