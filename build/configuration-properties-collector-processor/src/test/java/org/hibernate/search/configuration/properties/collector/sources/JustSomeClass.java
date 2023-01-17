/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.sources;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

public final class JustSomeClass {

	private JustSomeClass() {
	}

	@HibernateSearchConfiguration
	public static class InnerConfigurationSettings {

		/**
		 * Simple javadoc.
		 */
		public static final String SOME_INNER_SETTING = "some.inner.setting";

	}
}
