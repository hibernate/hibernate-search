/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

/**
 * Configuration properties for the Hibernate Search engine that are considered SPI (and not API).
 */
public class EngineSpiSettings {

	private EngineSpiSettings() {
	}

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
