/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import java.util.Properties;

import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

public final class HibernateSearch6DeprecationHelper {

	private HibernateSearch6DeprecationHelper() {
	}

	public static boolean isWarningEnabled(Properties properties) {
		return ConfigurationParseHelper.getBooleanValue( properties, "hibernate.search.v6_migration.deprecation_warnings", true );
	}

}
