/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg.spi;

import static org.hibernate.search.backend.lucene.resources.impl.DefaultLuceneWorkExecutorProvider.DEFAULT_BEAN_NAME;

import org.hibernate.search.backend.lucene.work.spi.LuceneWorkExecutorProvider;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;

/**
 * Configuration properties for the Hibernate Search Lucene backend that are considered SPI (and not API).
 */
public class LuceneBackendSpiSettings {

	private LuceneBackendSpiSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = EngineSettings.PREFIX + "backend.";
	/**
	 * The {@link LuceneWorkExecutorProvider} used to create work executors.
	 * <p>
	 * Expects a reference to a bean of type {@link LuceneWorkExecutorProvider}.
	 * <p>
	 * Defaults to {@link Defaults#BACKEND_WORK_EXECUTOR_PROVIDER}.
	 */
	public static final String BACKEND_WORK_EXECUTOR_PROVIDER = PREFIX + Radicals.BACKEND_WORK_EXECUTOR_PROVIDER;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}

		public static final String BACKEND_WORK_EXECUTOR_PROVIDER = "backend_work_executor_provider";

	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final BeanReference<? extends LuceneWorkExecutorProvider> BACKEND_WORK_EXECUTOR_PROVIDER =
				BeanReference.of( LuceneWorkExecutorProvider.class, DEFAULT_BEAN_NAME );

	}
}
