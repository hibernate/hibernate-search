/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.cfg;

import static java.lang.String.join;

import org.hibernate.search.backend.lucene.logging.impl.LuceneLogCategories;
import org.hibernate.search.backend.lucene.lowlevel.directory.FileSystemAccessStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.directory.LockingStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.index.IOStrategyName;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.util.InfoStream;

/**
 * Configuration properties for Lucene indexes.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.IndexSettings} for details.
 */
public final class LuceneIndexSettings {

	private LuceneIndexSettings() {
	}

	/**
	 * The prefix for directory-related property keys.
	 */
	public static final String DIRECTORY_PREFIX = "directory.";

	/**
	 * The type of directory to use when reading from or writing to the index.
	 * <p>
	 * Expects a String, such as "local-filesystem".
	 * See the reference documentation for a list of available values.
	 * <p>
	 * Defaults to {@link Defaults#DIRECTORY_TYPE}.
	 */
	public static final String DIRECTORY_TYPE = DIRECTORY_PREFIX + DirectoryRadicals.TYPE;

	/**
	 * The filesystem root for the directory.
	 * <p>
	 * Only available for the "local-filesystem" directory type.
	 * <p>
	 * Expects a String representing a path to an existing directory accessible in read and write mode, such as "local-filesystem".
	 * <p>
	 * The actual index files will be created in directory {@code <root>/<index-name>}.
	 * <p>
	 * Defaults to the JVM's working directory.
	 */
	public static final String DIRECTORY_ROOT = DIRECTORY_PREFIX + DirectoryRadicals.ROOT;

	/**
	 * How to lock on the directory.
	 * <p>
	 * Expects a {@link LockingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults are specific to each directory type.
	 */
	public static final String DIRECTORY_LOCKING_STRATEGY = DIRECTORY_PREFIX + DirectoryRadicals.LOCKING_STRATEGY;

	/**
	 * How to access the filesystem in the directory.
	 * <p>
	 * Only available for the "local-filesystem" directory type.
	 * <p>
	 * Expects a {@link FileSystemAccessStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#DIRECTORY_FILESYSTEM_ACCESS_STRATEGY}.
	 */
	public static final String DIRECTORY_FILESYSTEM_ACCESS_STRATEGY =
			DIRECTORY_PREFIX + DirectoryRadicals.FILESYSTEM_ACCESS_STRATEGY;

	/**
	 * The prefix for I/O-related property keys.
	 */
	public static final String IO_PREFIX = "io.";

	/**
	 * How to handle input/output, i.e. how to write to and read from indexes.
	 * <p>
	 * Expects a {@link IOStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_STRATEGY}.
	 */
	public static final String IO_STRATEGY = IO_PREFIX + IORadicals.STRATEGY;

	/**
	 * How much time may pass after an index change until the change is committed.
	 * <p>
	 * Only available for the "near-real-time" I/O strategy.
	 * <p>
	 * This effectively defines how long changes may be in an "unsafe" state,
	 * where a crash or power loss will result in data loss. For example:
	 * <ul>
	 *   <li>if set to 0, changes are safe as soon as the background process finishes treating a batch of changes.</li>
	 *   <li>if set to 1000, changes may not be safe for an additional 1 second
	 *   after the background process finishes treating a batch.
	 * 	 There is a benefit, though: index changes occurring less than 1 second after another change may execute faster.</li>
	 * </ul>
	 * <p>
	 * Note that individual write operations may trigger a forced commit
	 * (for example with the {@code write-sync} and {@code sync} indexing plan synchronization strategies in the ORM mapper),
	 * in which case you will only benefit from a non-zero commit interval during intensive indexing (mass indexer, ...).
	 * <p>
	 * Note that committing is <strong>not</strong> necessary to make changes visible to search queries:
	 * the two concepts are unrelated. See {@link #IO_REFRESH_INTERVAL}.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_COMMIT_INTERVAL}.
	 */
	public static final String IO_COMMIT_INTERVAL = IO_PREFIX + IORadicals.COMMIT_INTERVAL;

	/**
	 * How much time may pass after an index write
	 * until the index reader is considered stale and re-created.
	 * <p>
	 * Only available for the "near-real-time" I/O strategy.
	 * <p>
	 * This effectively defines how out-of-date search query results may be. For example:
	 * <ul>
	 *   <li>If set to 0, search results will always be completely in sync with the index writes.</li>
	 *   <li>If set to 1000, search results may reflect the state of the index at most 1 second ago.
	 * 	 There is a benefit, though: in situations where the index is being frequently written to,
	 * 	 search queries executed less than 1 second after another query may execute faster.</li>
	 * </ul>
	 * <p>
	 * Note that individual write operations may trigger a forced refresh
	 * (for example with the {@code read-sync} and {@code sync} indexing plan synchronization strategies in the ORM mapper),
	 * in which case you will only benefit from a non-zero refresh interval during intensive indexing (mass indexer, ...).
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_REFRESH_INTERVAL}.
	 */
	public static final String IO_REFRESH_INTERVAL = IO_PREFIX + IORadicals.REFRESH_INTERVAL;

	/**
	 * The prefix for property keys related to the index writer.
	 */
	public static final String IO_WRITER_PREFIX = IO_PREFIX + "writer.";

	/**
	 * The value to pass to {@link IndexWriterConfig#setMaxBufferedDocs(int)}.
	 * <p>
	 * Expects a positive Integer value,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see IndexWriterConfig#setMaxBufferedDocs(int)
	 */
	public static final String IO_WRITER_MAX_BUFFERED_DOCS = IO_WRITER_PREFIX + WriterRadicals.MAX_BUFFERED_DOCS;

	/**
	 * The value to pass to {@link IndexWriterConfig#setRAMBufferSizeMB(double)}.
	 * <p>
	 * Expects a positive Integer value in megabytes,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see IndexWriterConfig#setRAMBufferSizeMB(double)
	 */
	public static final String IO_WRITER_RAM_BUFFER_SIZE = IO_WRITER_PREFIX + WriterRadicals.RAM_BUFFER_SIZE;

	/**
	 * Whether to log the {@link IndexWriterConfig#setInfoStream(InfoStream)} (at the trace level) or not.
	 * <p>
	 * Logs are appended to the logger {@value LuceneLogCategories#INFOSTREAM_LOGGER_CATEGORY_NAME}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a String that can be parsed into such Boolean value.
	 * <p>
	 * Default is {@code false}.
	 *
	 * @see IndexWriterConfig#setInfoStream(InfoStream)
	 */
	public static final String IO_WRITER_INFOSTREAM = IO_WRITER_PREFIX + WriterRadicals.INFOSTREAM;

	/**
	 * The prefix for property keys related to merge.
	 */
	public static final String IO_MERGE_PREFIX = IO_PREFIX + "merge.";

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setMaxMergeDocs(int)}.
	 * <p>
	 * Expects a positive Integer value,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setMaxMergeDocs(int)
	 */
	public static final String IO_MERGE_MAX_DOCS = IO_MERGE_PREFIX + MergeRadicals.MAX_DOCS;

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setMergeFactor(int)}.
	 * <p>
	 * Expects a positive Integer value,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setMergeFactor(int)
	 */
	public static final String IO_MERGE_FACTOR = IO_MERGE_PREFIX + MergeRadicals.FACTOR;

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setMinMergeMB(double)}.
	 * <p>
	 * Expects a positive Integer value in megabytes,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setMinMergeMB(double)
	 */
	public static final String IO_MERGE_MIN_SIZE = IO_MERGE_PREFIX + MergeRadicals.MIN_SIZE;

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setMaxMergeMB(double)}.
	 * <p>
	 * Expects a positive Integer value in megabytes,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setMaxMergeMB(double)
	 */
	public static final String IO_MERGE_MAX_SIZE = IO_MERGE_PREFIX + MergeRadicals.MAX_SIZE;

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setMaxMergeMBForForcedMerge(double)}.
	 * <p>
	 * Expects a positive Integer value in megabytes,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setMaxMergeMBForForcedMerge(double)
	 */
	public static final String IO_MERGE_MAX_FORCED_SIZE = IO_MERGE_PREFIX + MergeRadicals.MAX_FORCED_SIZE;

	/**
	 * The value to pass to {@link LogByteSizeMergePolicy#setCalibrateSizeByDeletes(boolean)}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a String that can be parsed into such Boolean value.
	 * <p>
	 * The default for this setting is defined by Lucene.
	 *
	 * @see LogByteSizeMergePolicy#setCalibrateSizeByDeletes(boolean)
	 */
	public static final String IO_MERGE_CALIBRATE_BY_DELETES = IO_MERGE_PREFIX + MergeRadicals.CALIBRATE_BY_DELETES;

	/**
	 * The prefix for sharding-related property keys.
	 */
	public static final String SHARDING_PREFIX = "sharding.";

	/**
	 * The sharding strategy, deciding the number of shards, their identifiers,
	 * and how to translate a routing key into a shard identifier.
	 * <p>
	 * Expects a String, such as "hash".
	 * See the reference documentation for a list of available values.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#SHARDING_STRATEGY} (no sharding).
	 */
	public static final String SHARDING_STRATEGY = SHARDING_PREFIX + ShardingRadicals.STRATEGY;

	/**
	 * The number of shards to create for the index,
	 * i.e. the number of "physical" indexes, each holding a part of the index data.
	 * <p>
	 * Only available for the {@code hash} {@link #SHARDING_STRATEGY sharding strategy}.
	 * <p>
	 * Expects a strictly positive Integer value, such as 4,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * No default: this property must be set when using the {@code hash} sharding strategy.
	 */
	public static final String SHARDING_NUMBER_OF_SHARDS = SHARDING_PREFIX + ShardingRadicals.NUMBER_OF_SHARDS;

	/**
	 * The list of shard identifiers to accept for the index.
	 * <p>
	 * Only available for the {@code explicit} {@link #SHARDING_STRATEGY sharding strategy}.
	 * <p>
	 * Expects either a String containing multiple shard identifiers separated by commas (','),
	 * or a {@code Collection<String>} containing such shard identifiers.
	 * <p>
	 * No default: this property must be set when using the {@code explicit} sharding strategy.
	 */
	public static final String SHARDING_SHARD_IDENTIFIERS = SHARDING_PREFIX + ShardingRadicals.SHARD_IDENTIFIERS;

	/**
	 * The root property whose children are shards, e.g. {@code shards.0.<some shard-scoped property> = bar}
	 * or {@code shards.1.<some shard-scoped property> = bar} or {@code shards.main.<some shard-scoped property> = bar}.
	 */
	public static final String SHARDS = "shards";

	/**
	 * The prefix for indexing-related property keys.
	 */
	public static final String INDEXING_PREFIX = "indexing.";

	/**
	 * The number of indexing queues assigned to each index (or each shard of each index, when sharding is enabled).
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed into an integer value.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Indexing",
	 * for more information about this setting and its implications.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_COUNT}.
	 */
	public static final String INDEXING_QUEUE_COUNT = INDEXING_PREFIX + IndexingRadicals.QUEUE_COUNT;

	/**
	 * The size of indexing queues.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed into an integer value.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Indexing",
	 * for more information about this setting and its implications.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_SIZE}.
	 */
	public static final String INDEXING_QUEUE_SIZE = INDEXING_PREFIX + IndexingRadicals.QUEUE_SIZE;

	/**
	 * Builds a configuration property key for the given shard of all indexes of the default backend,
	 * with the given radical.
	 * <p>
	 * See constants in this class for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backend.shard.<shardId>.indexing.queue_count}"
	 *
	 * @param shardId The identifier of the shard to configure.
	 * @param radical The radical of the configuration property (see constants in this class).
	 * @return the concatenated shard settings key
	 */
	public static String shardKey(String shardId, String radical) {
		return join( ".", EngineSettings.BACKEND, SHARDS, shardId, radical );
	}

	/**
	 * Builds a configuration property key for the given shard of the given index of the default backend,
	 * with the given radical.
	 * <p>
	 * See constants in this class for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backends.<backendName>.indexes.<indexName>.shard.<shardId>.indexing.queue_count}"
	 *
	 * @param indexName The name of the index in which the shard to configure is located.
	 * @param shardId The identifier of the shard to configure.
	 * @param radical The radical of the configuration property (see constants in this class).
	 * @return the concatenated shard settings key
	 */
	public static String shardKey(String indexName, String shardId, String radical) {
		if ( indexName == null ) {
			return shardKey( shardId, radical );
		}
		return join( ".", EngineSettings.BACKEND, BackendSettings.INDEXES, indexName,
				SHARDS, shardId, radical
		);
	}

	/**
	 * Builds a configuration property key for the given shard of the given index of the given backend,
	 * with the given radical.
	 * <p>
	 * See constants in this class for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backends.<backendName>.indexes.<indexName>.shard.<shardId>.indexing.queue_count}"
	 *
	 * @param backendName The name of the backend in which the shard to configure is located.
	 * @param indexName The name of the index in which the shard to configure is located.
	 * @param shardId The identifier of the shard to configure.
	 * @param radical The radical of the configuration property (see constants in this class).
	 * @return the concatenated shard settings key
	 */
	public static String shardKey(String backendName, String indexName, String shardId, String radical) {
		if ( backendName == null ) {
			return shardKey( indexName, shardId, radical );
		}
		if ( indexName == null ) {
			return join( ".", EngineSettings.BACKENDS, backendName, SHARDS, shardId, radical );
		}
		return join( ".", EngineSettings.BACKENDS, backendName, BackendSettings.INDEXES, indexName,
				SHARDS, shardId, radical
		);
	}

	/**
	 * Configuration property keys for directories without the {@link #DIRECTORY_PREFIX prefix}.
	 */
	public static final class DirectoryRadicals {

		private DirectoryRadicals() {
		}

		public static final String TYPE = "type";
		public static final String ROOT = "root";
		public static final String LOCKING_STRATEGY = "locking.strategy";
		public static final String FILESYSTEM_ACCESS_STRATEGY = "filesystem_access.strategy";
	}

	/**
	 * Configuration property keys for I/O, without the {@link #IO_PREFIX prefix}.
	 */
	public static final class IORadicals {

		private IORadicals() {
		}

		public static final String STRATEGY = "strategy";
		public static final String COMMIT_INTERVAL = "commit_interval";
		public static final String REFRESH_INTERVAL = "refresh_interval";
	}

	/**
	 * Configuration property keys for index writer options, without the {@link #IO_WRITER_PREFIX prefix}.
	 */
	public static final class WriterRadicals {

		private WriterRadicals() {
		}

		public static final String MAX_BUFFERED_DOCS = "max_buffered_docs";
		public static final String RAM_BUFFER_SIZE = "ram_buffer_size";
		public static final String INFOSTREAM = "infostream";

	}

	/**
	 * Configuration property keys for merge options, without the {@link #IO_MERGE_PREFIX prefix}.
	 */
	public static final class MergeRadicals {

		private MergeRadicals() {
		}

		public static final String MAX_DOCS = "max_docs";
		public static final String FACTOR = "factor";
		public static final String MIN_SIZE = "min_size";
		public static final String MAX_SIZE = "max_size";
		public static final String MAX_FORCED_SIZE = "max_forced_size";
		public static final String CALIBRATE_BY_DELETES = "calibrate_by_deletes";

	}

	/**
	 * Configuration property keys for sharding, without the {@link #SHARDING_PREFIX prefix}.
	 */
	public static final class ShardingRadicals {

		private ShardingRadicals() {
		}

		public static final String STRATEGY = "strategy";
		public static final String NUMBER_OF_SHARDS = "number_of_shards";
		public static final String SHARD_IDENTIFIERS = "shard_identifiers";
	}

	/**
	 * Configuration property keys for indexing, without the {@link #INDEXING_PREFIX prefix}.
	 */
	public static final class IndexingRadicals {

		private IndexingRadicals() {
		}

		public static final String QUEUE_COUNT = "queue_count";
		public static final String QUEUE_SIZE = "queue_size";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final String DIRECTORY_TYPE = "local-filesystem";
		public static final String DIRECTORY_ROOT = ".";
		public static final FileSystemAccessStrategyName DIRECTORY_FILESYSTEM_ACCESS_STRATEGY =
				FileSystemAccessStrategyName.AUTO;
		public static final String SHARDING_STRATEGY = "none";
		public static final IOStrategyName IO_STRATEGY = IOStrategyName.NEAR_REAL_TIME;
		public static final int IO_COMMIT_INTERVAL = 1000;
		public static final int IO_REFRESH_INTERVAL = 0;
		public static final int INDEXING_QUEUE_COUNT = 10;
		public static final int INDEXING_QUEUE_SIZE = 1000;
	}
}
