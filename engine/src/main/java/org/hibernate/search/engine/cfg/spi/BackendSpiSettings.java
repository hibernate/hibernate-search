/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import org.hibernate.search.engine.backend.work.execution.spi.BackendWorkExecutorProvider;
import org.hibernate.search.engine.cfg.EngineSettings;

/**
 * Configuration properties for the Hibernate Search backend that are considered SPI (and not API).
 */
public class BackendSpiSettings {

	private BackendSpiSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = EngineSettings.PREFIX + "backend.";
	/**
	 * The {@link BackendWorkExecutorProvider} used to create work executors.
	 * <p>
	 * Expects a reference to a bean of type {@link BackendWorkExecutorProvider}.
	 * <p>
	 * Default value is backend-specific providing a built-in work executor provider.
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
	}
}
