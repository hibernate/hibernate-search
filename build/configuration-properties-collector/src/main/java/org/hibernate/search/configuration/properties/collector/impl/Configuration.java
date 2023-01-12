/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

/**
 * Lists the config parameters that can be passed to this annotation processor via {@code -A.....}.
 */
public final class Configuration {
	private Configuration() {
	}

	/**
	 * Use to define a property prefix for entire module. Accepts a list of comma seperated prefixes.
	 * Can be overriden with {@link HibernateSearchConfiguration} on class and property levels if needed.
	 */
	public static final String PROPERTY_PREFIX = "org.hibernate.search.cpcap.property.prefix";
	/**
	 * Use to define a table title for generated asciidoc table and as a value in corresponding table column.
	 */
	public static final String MODULE_NAME = "org.hibernate.search.cpcap.module.name";
	/**
	 * Use to define a base URL for Hibernate Search Javadoc. As we are getting parts of Javadoc links in it should
	 * be adjusted to point to somewhere where the docs actually live.
	 */
	public static final String JAVADOC_LINK = "org.hibernate.search.cpcap.javadoc.link";
	/**
	 * Use to define a pattern for classes to be ignored by this collector. We can have some {@code *Settings} classes
	 * in {@code impl} packages. And we don't need to collect properties from those.
	 */
	public static final String IGNORE_PATTERN = "org.hibernate.search.cpcap.ignore.pattern";
	/**
	 * Use to define a pattern for property key values that should be ignored. By default, we will ignore keys that end
	 * with a dot {@code '.'}.
	 */
	public static final String IGNORE_KEY_VALUE_PATTERN = "org.hibernate.search.cpcap.ignore.key.value.pattern";
	/**
	 * Use to define a name for generated json/asciidoc files. Files should have unique names if we want to be able to
	 * unpack multiple source jars to include in reference documentation.
	 */
	public static final String GENERATED_FILE_NAME = "org.hibernate.search.cpcap.generated.filename";
}
