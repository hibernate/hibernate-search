/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.cfg.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

@Incubating
@HibernateSearchConfiguration(
		title = "Hibernate Search Mapper - POJO Standalone",
		anchorPrefix = "hibernate-search-mapper-pojo-standalone-"
)
public final class StandalonePojoMapperSpiSettings {

	private StandalonePojoMapperSpiSettings() {
	}

	public static final String PREFIX = "hibernate.search.";

	public static final String BEAN_PROVIDER = PREFIX + Radicals.BEAN_PROVIDER;

	public static class Radicals {

		private Radicals() {
		}

		public static final String BEAN_PROVIDER = "bean_provider";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}
	}

}
