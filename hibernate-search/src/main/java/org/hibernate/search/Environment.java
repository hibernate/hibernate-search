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

	private Environment() {
	}

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

	public static final String WORKER_PREFIX = "hibernate.search.worker.";
	public static final String WORKER_SCOPE = WORKER_PREFIX + "scope";
	public static final String WORKER_BACKEND = WORKER_PREFIX + "backend";
	public static final String WORKER_EXECUTION = WORKER_PREFIX + "execution";

	/**
	 * Defines the maximum number of indexing operation batched per transaction
	 */
	public static final String WORKER_BATCHSIZE = WORKER_PREFIX + "batch_size";

	/**
	 * only used then execution is async
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
	public static final String READER_PREFIX = "hibernate.search.reader.";

	/**
	 * define the reader strategy used
	 */
	public static final String READER_STRATEGY = READER_PREFIX + "strategy";

	/**
	 * filter caching strategy class (must have a no-arg constructor and implement FilterCachingStrategy)
	 */
	public static final String FILTER_CACHING_STRATEGY = "hibernate.search.filter.cache_strategy";

	/**
	 * number of docidresults cached in hard reference.
	 */
	public static final String CACHE_DOCIDRESULTS_SIZE = "hibernate.search.filter.cache_docidresults.size";

	/**
	 * batch backend implementation class (must have a no-arg constructor and implement BatchBackend)
	 * also prefix for configuration settings of the batch backend
	 */
	public static final String BATCH_BACKEND = "hibernate.search.batchbackend";

	/**
	 * When set to true a lock on the index will not be released until the
	 * SearchFactory (or SessionFactory) is closed.
	 * This improves performance in applying changes to the index, but no other application
	 * can access the index in write mode while Hibernate Search is running.
	 * This is an index-scoped property and defaults to false.
	 */
	public static final String EXCLUSIVE_INDEX_USE = "exclusive_index_use";

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
	 * If set to {@code true} the search statistic will be gathered.
	 */
	public static final String GENERATE_STATS = "hibernate.search.generate_statistics";

	/**
	 * The Lucene match version parameter. Needed since Lucene 3.x
	 */
	public static final String LUCENE_MATCH_VERSION = "hibernate.search.lucene_version";
}
