/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

/**
 * Configuration properties for the Hibernate Search engine that are considered SPI (and not API).
 */
public class EngineSpiSettings {

	private EngineSpiSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property.
	 */
	public static final String PREFIX = EngineSettings.PREFIX;

	/**
	 * The {@link BeanConfigurer} instances used to programmatically assign names to beans.
	 * <p>
	 * Expects a multi-valued reference to a bean of type {@link BeanConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "multi-valued bean reference" properties and accepted values.
	 */
	public static final String BEAN_CONFIGURERS = PREFIX + Radicals.BEAN_CONFIGURERS;

	/**
	 * The {@link ThreadProvider} used to create threads.
	 * <p>
	 * Expects a reference to a bean of type {@link ThreadProvider}.
	 * <p>
	 * Defaults to {@link Defaults#THREAD_PROVIDER}, an embedded thread provider.
	 */
	public static final String THREAD_PROVIDER = PREFIX + Radicals.THREAD_PROVIDER;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}

		public static final String BEAN_CONFIGURERS = "bean_configurers";
		public static final String THREAD_PROVIDER = "thread_provider";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<BeanReference<? extends BeanConfigurer>> BEAN_CONFIGURERS = Collections.emptyList();
		public static final BeanReference<? extends ThreadProvider> THREAD_PROVIDER =
				BeanReference.of( ThreadProvider.class, EmbeddedThreadProvider.NAME );
	}
}
