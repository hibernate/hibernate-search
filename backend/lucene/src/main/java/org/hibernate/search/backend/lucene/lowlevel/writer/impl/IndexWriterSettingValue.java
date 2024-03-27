/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.BiConsumer;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;

class IndexWriterSettingValue<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String settingName;
	private final T value;
	private final BiConsumer<IndexWriterConfig, T> writerSettingApplier;
	private final BiConsumer<LogByteSizeMergePolicy, T> mergePolicySettingApplier;

	IndexWriterSettingValue(String settingName, T value,
			BiConsumer<IndexWriterConfig, T> writerSettingApplier,
			BiConsumer<LogByteSizeMergePolicy, T> mergePolicySettingApplier) {
		this.settingName = settingName;
		this.value = value;
		this.writerSettingApplier = writerSettingApplier;
		this.mergePolicySettingApplier = mergePolicySettingApplier;
	}

	@Override
	public String toString() {
		return "<" + settingName + "=" + value + ">";
	}

	/**
	 * @param writerConfig the {@link IndexWriterConfig}
	 * @throws SearchException when user selects an invalid value.
	 */
	public void applySetting(IndexWriterConfig writerConfig) {
		try {
			writerSettingApplier.accept( writerConfig, value );
		}
		catch (RuntimeException e) {
			throw log.illegalIndexWriterSetting( settingName, value, e.getMessage(), e );
		}
	}

	/**
	 * @param logByteSizeMergePolicy the {@link LogByteSizeMergePolicy}
	 * @throws SearchException when user selects an invalid value.
	 */
	public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy) {
		try {
			mergePolicySettingApplier.accept( logByteSizeMergePolicy, value );
		}
		catch (RuntimeException e) {
			throw log.illegalMergePolicySetting( settingName, value, e.getMessage(), e );
		}
	}
}
