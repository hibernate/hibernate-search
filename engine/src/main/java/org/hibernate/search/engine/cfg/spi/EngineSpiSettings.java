/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.common.spi.LogErrorHandler;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

/**
 * Configuration properties for the Hibernate Search engine that are considered SPI (and not API).
 */
public class EngineSpiSettings {

	private EngineSpiSettings() {
	}

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
	public static final String BEAN_CONFIGURERS = "bean_configurers";

	/**
	 * The {@link org.hibernate.search.engine.common.spi.ErrorHandler} instance to use at runtime.
	 * <p>
	 * Expects a reference to a bean of type {@link org.hibernate.search.engine.common.spi.ErrorHandler}.
	 * <p>
	 * Defaults to a logging handler.
	 */
	public static final String ERROR_HANDLER = "error_handler";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<BeanReference<? extends BeanConfigurer>> BEAN_CONFIGURERS = Collections.emptyList();

		public static final BeanReference<? extends ErrorHandler> ERROR_HANDLER = BeanReference.of( LogErrorHandler.class );
	}
}
