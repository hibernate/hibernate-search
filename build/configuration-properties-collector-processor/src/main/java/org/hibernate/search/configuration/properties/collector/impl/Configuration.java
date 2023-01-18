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

	private static final String HIBERNATE_SEARCH_CPCAP_PREFIX = "org.hibernate.search.cpcap.";

	/**
	 * Use to define a property prefix for entire module. Accepts a list of comma seperated prefixes.
	 * Can be overriden with {@link HibernateSearchConfiguration} on class and property levels if needed.
	 */
	public static final String PROPERTY_PREFIX = HIBERNATE_SEARCH_CPCAP_PREFIX + "property.prefix";
	/**
	 * Use to define a base URL for Hibernate Search Javadoc. As we are getting parts of Javadoc links in it should
	 * be adjusted to point to somewhere where the docs actually live.
	 */
	public static final String JAVADOC_LINK = HIBERNATE_SEARCH_CPCAP_PREFIX + "javadoc.link";
	/**
	 * Use to define a pattern for classes to be ignored by this collector. We can have some {@code *Settings} classes
	 * in {@code impl} packages. And we don't need to collect properties from those.
	 */
	public static final String IGNORE_PATTERN = HIBERNATE_SEARCH_CPCAP_PREFIX + "ignore.pattern";
	/**
	 * Use to define a pattern for property key values that should be ignored. By default, we will ignore keys that end
	 * with a dot {@code '.'}.
	 */
	public static final String IGNORE_KEY_VALUE_PATTERN = HIBERNATE_SEARCH_CPCAP_PREFIX + "ignore.key.value.pattern";
	/**
	 * Use to define a name for generated json/asciidoc files. Files should have unique names if we want to be able to
	 * unpack multiple source jars to include in reference documentation.
	 */
	public static final String GENERATED_FILE_NAME = HIBERNATE_SEARCH_CPCAP_PREFIX + "generated.filename";
}
