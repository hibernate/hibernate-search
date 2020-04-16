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
 * A source of index writer configuration that can be re-used on multiple writers.
 * <p>
 * This is mostly necessary because we don't have access to the user configuration after startup,
 * and we don't want to re-use the same IndexWriterConfig instance multiple times in order to be safe
 * and make sure a previous, failing index writer will never affect the configuration of
 * the new index writer created to replace it.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class IndexWriterConfigSource {

	// property path keywords
	public static final String PROP_GROUP = "indexwriter";

	private final List<IndexWriterSettingValue<?>> values;

	public IndexWriterConfigSource(ConfigurationPropertySource propertySource) {
		ConfigurationPropertySource indexingParameters = propertySource.withMask( PROP_GROUP );
		values = IndexWriterSettings.extractAll( indexingParameters );
	}

	@Override
	public String toString() {
		return "IndexWriterConfigSource{" + values + '}';
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
