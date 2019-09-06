/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Set;

/**
 * A source of property values for Hibernate Search with knowledge of the full set of properties.
 * <p>
 * Implementations provide, on top of the usual key lookup,
 * a way to retrieve <strong>all</strong> keys with a given prefix,
 * which allows checking that all property keys were consumed, in particular.
 */
public interface AllAwareConfigurationPropertySource extends ConfigurationPropertySource {

	Set<String> resolveAll(String prefix);

}
