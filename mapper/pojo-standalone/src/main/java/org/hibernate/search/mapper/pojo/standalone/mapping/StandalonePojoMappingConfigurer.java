/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An object responsible for configuring the Hibernate Search mapping.
 * <p>
 * Users can select a mapping configurer through the
 * {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings#MAPPING_CONFIGURER configuration properties}.
 */

@Incubating
public interface StandalonePojoMappingConfigurer {

	/**
	 * Configure the Hibernate Search mapping as necessary using the given {@code context}.
	 *
	 * @param context A context exposing methods to configure the mapping.
	 */
	void configure(StandalonePojoMappingConfigurationContext context);
}
