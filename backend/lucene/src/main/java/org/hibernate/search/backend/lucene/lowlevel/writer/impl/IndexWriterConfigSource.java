/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.util.List;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.apache.lucene.analysis.Analyzer;
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

	public static IndexWriterConfigSource create(Analyzer analyzer, ConfigurationPropertySource propertySource) {
		List<IndexWriterSettingValue<?>> values = IndexWriterSettings.extractAll( propertySource );
		return new IndexWriterConfigSource( analyzer, values );
	}

	private final Analyzer analyzer;
	private final List<IndexWriterSettingValue<?>> values;

	private IndexWriterConfigSource(Analyzer analyzer, List<IndexWriterSettingValue<?>> values) {
		this.analyzer = analyzer;
		this.values = values;
	}

	@Override
	public String toString() {
		return "IndexWriterConfigSource{" + analyzer + "," + values + '}';
	}

	/**
	 * Creates a new {@link IndexWriterConfig}.
	 * Undefined parameters are not set, leaving the lucene default.
	 */
	public IndexWriterConfig createIndexWriterConfig() {
		IndexWriterConfig writerConfig = new IndexWriterConfig( analyzer );
		for ( IndexWriterSettingValue<?> value : values ) {
			value.applySetting( writerConfig );
		}
		writerConfig.setMergePolicy( createMergePolicy() );
		return writerConfig;
	}

	private LogByteSizeMergePolicy createMergePolicy() {
		LogByteSizeMergePolicy logByteSizeMergePolicy = new LogByteSizeMergePolicy();
		for ( IndexWriterSettingValue<?> value : values ) {
			value.applySetting( logByteSizeMergePolicy );
		}
		return logByteSizeMergePolicy;
	}

}
