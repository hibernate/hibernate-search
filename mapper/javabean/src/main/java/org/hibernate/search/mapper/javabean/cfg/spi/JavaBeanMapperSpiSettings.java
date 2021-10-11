/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.cfg.spi;

import org.hibernate.search.mapper.javabean.schema.management.SchemaManagementStrategyName;

public final class JavaBeanMapperSpiSettings {

	private JavaBeanMapperSpiSettings() {
	}

	public static final String PREFIX = "hibernate.search.";

	public static final String BEAN_PROVIDER = PREFIX + Radicals.BEAN_PROVIDER;

	/**
	 * The schema management strategy, controlling how indexes and their schema
	 * are created, updated, validated or dropped on startup and shutdown.
	 * <p>
	 * Expects a {@link SchemaManagementStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#SCHEMA_MANAGEMENT_STRATEGY}.
	 *
	 * @see SchemaManagementStrategyName
	 */
	public static final String SCHEMA_MANAGEMENT_STRATEGY = PREFIX + Radicals.SCHEMA_MANAGEMENT_STRATEGY;

	public static class Radicals {

		private Radicals() {
		}

		public static final String BEAN_PROVIDER = "bean_provider";
		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY = SchemaManagementStrategyName.CREATE_OR_VALIDATE;
	}

}
