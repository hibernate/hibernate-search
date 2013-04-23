/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Environment {

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
	public static final String WORKER_PREFIX = "worker.";
	public static final String WORKER_BACKEND = WORKER_PREFIX + "backend";
	public static final String WORKER_EXECUTION = WORKER_PREFIX + "execution";

	/**
	 * Defines the maximum number of indexing operation batched per transaction
	 */
	public static final String QUEUEINGPROCESSOR_BATCHSIZE = "hibernate.search.batch_size";

	/**
	 * Thread pool size
	 * default 1
	 */
	public static final String WORKER_THREADPOOL_SIZE = Environment.WORKER_PREFIX + "thread_pool.size";

	/**
	 * Size of the buffer queue (besides the thread pool size)
	 * <ul>
	 * <li>only used then execution is async</li>
	 * <li>default infinite</li>
	 * </ul>
	 */
	public static final String WORKER_WORKQUEUE_SIZE = Environment.WORKER_PREFIX + "buffer_queue.max";

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
	 * SearchFactory (or SessionFactory) is closed.
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
	 * <p/>
	 * Note that we try again after 5 seconds.
	 * <p/>
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
	 * Set to a fully qualified classname of a type implementing org.hibernate.search.exception.ErrorHandler
	 * to override the error strategy used during processing of the Lucene updates.
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
	 * The Lucene match version parameter. Needed since Lucene 3.x
	 */
	public static final String LUCENE_MATCH_VERSION = "hibernate.search.lucene_version";

	/**
	 * Parameter name used to configure the default null token
	 */
	public static final String DEFAULT_NULL_TOKEN = "hibernate.search.default_null_token";

	/**
	 * Tokenizer or filter parameter describing the charset used to load associated resources if needed.
	 */
	public static final String RESOURCE_CHARSET = "resource_charset";

	/**
	 * When enabled reindexing of an entity is skipped if the updates affect only non-indexed fields.
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
	 * If nothing else is specified we use {@code Version.LUCENE_CURRENT} as the default Lucene version. This version
	 * parameter was introduced by Lucene to attempt providing backwards compatibility when upgrading Lucene versions
	 * and not wanting to rebuild the index from scratch. It's highly recommended to specify a version, so that you
	 * can upgrade Hibernate Search and control when to eventually upgrade the Lucene format.
	 */
	@SuppressWarnings("deprecation")//We know it's discouraged
	public static final org.apache.lucene.util.Version DEFAULT_LUCENE_MATCH_VERSION = org.apache.lucene.util.Version.LUCENE_CURRENT;

	/**
	 * Used to specify an alternative IndexManager implementation for a specific index.
	 * This is an index scoped property, so it needs to be prefixed by default or the index name, for example:
	 * <ul>
	 * <li>hibernate.search.default.indexmanager</li>
	 * <li>hibernate.search.Books.indexmanager</li>
	 * </ul>
	 */
	public static final String INDEX_MANAGER_IMPL_NAME = "indexmanager";

	private Environment() {
	}

}
