/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

/**
 * @deprecated This SPI was introduced by mistake and was never used.
 */
@Deprecated
public interface AutomaticIndexingStrategyStartContext {

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	/**
	 * @return A configuration property source, appropriately masked so that the strategy
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "synchronization" and any sub-keys are reserved.
	 */
	ConfigurationPropertySource configurationPropertySource();

}
