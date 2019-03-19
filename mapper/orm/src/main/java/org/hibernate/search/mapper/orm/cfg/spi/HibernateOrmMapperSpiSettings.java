/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.spi;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public final class HibernateOrmMapperSpiSettings {

	private HibernateOrmMapperSpiSettings() {
	}

	public static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	/**
	 * The factory to use to create {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle} instances.
	 * <p>
	 * Expects a {@link HibernateOrmPropertyHandleFactoryName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#PROPERTY_HANDLE_FACTORY}.
	 */
	public static final String PROPERTY_HANDLE_FACTORY = PREFIX + Radicals.PROPERTY_HANDLE_FACTORY;

	public static class Radicals {

		private Radicals() {
		}

		public static final String PROPERTY_HANDLE_FACTORY = "property_handle_factory";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final HibernateOrmPropertyHandleFactoryName PROPERTY_HANDLE_FACTORY = HibernateOrmPropertyHandleFactoryName.METHOD_HANDLE;
	}

}
