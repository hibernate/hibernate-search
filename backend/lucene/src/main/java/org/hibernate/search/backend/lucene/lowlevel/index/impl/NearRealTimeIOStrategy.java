/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.NearRealTimeIndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterConfigSource;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.reporting.EventContext;

public class NearRealTimeIOStrategy extends IOStrategy {

	private static final ConfigurationProperty<Integer> COMMIT_INTERVAL =
			ConfigurationProperty.forKey( LuceneIndexSettings.IO_COMMIT_INTERVAL )
					.asIntegerPositiveOrZero()
					.withDefault( LuceneIndexSettings.Defaults.IO_COMMIT_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> REFRESH_INTERVAL =
			ConfigurationProperty.forKey( LuceneIndexSettings.IO_REFRESH_INTERVAL )
					.asIntegerPositiveOrZero()
					.withDefault( LuceneIndexSettings.Defaults.IO_REFRESH_INTERVAL )
					.build();

	public static NearRealTimeIOStrategy create(ConfigurationPropertySource propertySource,
			TimingSource timingSource, BackendThreads threads, FailureHandler failureHandler) {
		int commitInterval = COMMIT_INTERVAL.get( propertySource );
		int refreshInterval = REFRESH_INTERVAL.get( propertySource );
		return new NearRealTimeIOStrategy(
				timingSource, commitInterval, refreshInterval,
				threads, failureHandler
		);
	}

	private final TimingSource timingSource;
	private final int commitInterval;
	private final int refreshInterval;

	private NearRealTimeIOStrategy(TimingSource timingSource, int commitInterval, int refreshInterval,
			BackendThreads threads,
			FailureHandler failureHandler) {
		super( threads, failureHandler );
		this.timingSource = timingSource;
		this.commitInterval = commitInterval;
		this.refreshInterval = refreshInterval;
	}

	@Override
	IndexWriterProvider createIndexWriterProvider(String indexName, EventContext eventContext,
			DirectoryHolder directoryHolder, IndexWriterConfigSource configSource) {
		if ( commitInterval != 0 ) {
			timingSource.ensureTimeEstimateIsInitialized();
		}
		return new IndexWriterProvider(
				indexName, eventContext,
				directoryHolder, configSource,
				timingSource, commitInterval, threads,
				failureHandler
		);
	}

	@Override
	IndexReaderProvider createIndexReaderProvider(DirectoryHolder directoryHolder,
			IndexWriterProvider indexWriterProvider) {
		if ( refreshInterval != 0 ) {
			timingSource.ensureTimeEstimateIsInitialized();
		}
		return new NearRealTimeIndexReaderProvider( indexWriterProvider, timingSource, refreshInterval );
	}

}
