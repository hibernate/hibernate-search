/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;

/**
 * Configuration properties for the Hibernate Search engine.
 */
public final class SearchEngineSettings {

	private SearchEngineSettings() {
	}

	/**
	 * The {@link BeanConfigurer} instances
	 * used to pre-define beans.
	 * <p>
	 * Accepts either:
	 * <ul>
	 *     <li>A list, containing {@link BeanConfigurer} instances,
	 *     or {@link BeanConfigurer} implementation classes to be resolved against the {@link BeanResolver},
	 *     or Strings to be considered as bean name and resolved against the {@link BeanResolver}
	 *     (fully-qualified class names will be instantiated using reflection).
	 *     </li>
	 *     <li>A String containing whitespace-separated bean names to be resolved against the {@link BeanResolver}
	 *     (fully-qualified class names will be instantiated using reflection).</li>
	 * </ul>
	 */
	public static final String BEAN_CONFIGURERS = "bean_configurers";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<BeanReference<? extends BeanConfigurer>> BEAN_CONFIGURERS = Collections.emptyList();
	}
}
