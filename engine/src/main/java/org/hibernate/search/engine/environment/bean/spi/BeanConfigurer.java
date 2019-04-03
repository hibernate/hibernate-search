/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

/**
 * An object responsible for defining beans that can then be resolved during Hibernate Search bootstrap.
 * <p>
 * Bean configurers can be enabled through two different methods:
 * <ul>
 *     <li>Java services: create a file named {@code org.hibernate.search.engine.environment.bean.spi.BeanConfigurer}
 *     in the {@code META-INF/services} directory of your JAR,
 *     and set the content of this file to the fully-qualified name of your {@link BeanConfigurer} implementation.
 *     <li>Configuration properties: set the {@link org.hibernate.search.engine.cfg.spi.EngineSpiSettings#BEAN_CONFIGURERS}
 *     configuration property (be sure to use the appropriate prefix for the property key, e.g. {@code hibernate.search.}).
 * </ul>
 */
public interface BeanConfigurer {

	/**
	 * Configure beans as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure beans.
	 */
	void configure(BeanConfigurationContext context);

}
