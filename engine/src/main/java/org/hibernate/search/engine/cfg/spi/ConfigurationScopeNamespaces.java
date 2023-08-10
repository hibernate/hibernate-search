/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Names of configuration scope namespaces used to construct a {@link ConfigurationScope}.
 *
 * @see ConfigurationScope
 * @see	ConfigurationProvider
 */
@Incubating
public final class ConfigurationScopeNamespaces {
	private ConfigurationScopeNamespaces() {
	}

	public static final String GLOBAL = "global";
	public static final String BACKEND = "backend";
	public static final String INDEX = "index";
}
