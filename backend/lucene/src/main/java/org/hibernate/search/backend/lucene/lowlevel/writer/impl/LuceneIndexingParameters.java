/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.util.List;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;

/**
 * Wrapper class around the Lucene indexing parameters defined in IndexWriterSetting.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters {

	// property path keywords
	public static final String PROP_GROUP = "indexwriter";

	private final List<IndexWriterSettingValue<?>> values;

	public LuceneIndexingParameters(ConfigurationPropertySource propertySource) {
		ConfigurationPropertySource indexingParameters = propertySource.withMask( PROP_GROUP );
		values = IndexWriterSettings.extractAll( indexingParameters );
	}

	@Override
	public String toString() {
		return "LuceneIndexingParameters{" + values + '}';
	}

	/**
	 * Applies the parameters represented by this to a writer.
	 * Undefined parameters are not set, leaving the lucene default.
	 *
	 * @param writerConfig the IndexWriter configuration whereto the parameters will be applied.
	 */
	public void applyToWriter(IndexWriterConfig writerConfig) {
		for ( IndexWriterSettingValue<?> value : values ) {
			value.applySetting( writerConfig );
		}
		writerConfig.setMergePolicy( createMergePolicy() );
	}

	private LogByteSizeMergePolicy createMergePolicy() {
		LogByteSizeMergePolicy logByteSizeMergePolicy = new LogByteSizeMergePolicy();
		for ( IndexWriterSettingValue<?> value : values ) {
			value.applySetting( logByteSizeMergePolicy );
		}
		return logByteSizeMergePolicy;
	}

}
