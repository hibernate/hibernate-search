/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.cfg.spi;

import static org.hibernate.search.backend.elasticsearch.resources.impl.DefaultElasticsearchWorkExecutorProvider.DEFAULT_BEAN_NAME;

import org.hibernate.search.backend.elasticsearch.work.spi.ElasticsearchWorkExecutorProvider;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Configuration properties for the Elasticsearch backend that are considered SPI (and not API).
 */
public final class ElasticsearchBackendSpiSettings {

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = EngineSettings.PREFIX + "backend.";
	/**
	 * The {@link ElasticsearchWorkExecutorProvider} used to create work executors.
	 * <p>
	 * Expects a reference to a bean of type {@link ElasticsearchWorkExecutorProvider}.
	 * <p>
	 * Defaults to {@link Defaults#BACKEND_WORK_EXECUTOR_PROVIDER}.
	 */
	public static final String BACKEND_WORK_EXECUTOR_PROVIDER = PREFIX + Radicals.BACKEND_WORK_EXECUTOR_PROVIDER;

	@Incubating
	public static final String CLIENT_FACTORY = PREFIX + Radicals.CLIENT_FACTORY;

	private ElasticsearchBackendSpiSettings() {
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}

		public static final String BACKEND_WORK_EXECUTOR_PROVIDER = "backend_work_executor_provider";
		public static final String CLIENT_FACTORY = "client_factory";
	}

	public static final class Defaults {

		private Defaults() {
		}

		public static final BeanReference<? extends ElasticsearchWorkExecutorProvider> BACKEND_WORK_EXECUTOR_PROVIDER =
				BeanReference.of( ElasticsearchWorkExecutorProvider.class, DEFAULT_BEAN_NAME );
	}
}
