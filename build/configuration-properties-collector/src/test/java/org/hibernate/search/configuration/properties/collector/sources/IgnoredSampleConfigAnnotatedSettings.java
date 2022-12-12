/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.sources;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

@HibernateSearchConfiguration(ignore = true)
public final class IgnoredSampleConfigAnnotatedSettings {

	private IgnoredSampleConfigAnnotatedSettings() {
	}

	/**
	 * Some javadoc explaining the property.
	 * <p>
	 * Defaults to {@value Defaults#SOME_PROPERTY_KEY_DEFAULT_VALUE} as value.
	 * <p>
	 * Defaults to {@link Defaults#SOME_PROPERTY_KEY_DEFAULT_VALUE} as link.
	 */
	public static final String SOME_PROPERTY_KEY = "some.property.key";

	public static class Defaults {
		public static final String SOME_PROPERTY_KEY_DEFAULT_VALUE = "default";
	}
}
