/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.sources;

import org.hibernate.search.configuration.properties.collector.sources.inner.level2.SomePropertiesToReference;
import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

public final class SampleConfigNotAnnotatedSettings {

	private SampleConfigNotAnnotatedSettings() {
	}

	/**
	 * Some javadoc explaining the property.
	 * <p>
	 * Defaults to {@value Defaults#SOME_OTHER_PROPERTY_KEY} as value.
	 * <p>
	 * Defaults to {@link Defaults#SOME_OTHER_PROPERTY_KEY} as link.
	 */
	public static final String SOME_OTHER_PROPERTY_KEY = "some.other.property.key";

	/**
	 * Some javadoc explaining the property.
	 * <p>
	 * Just some links to other classes:
	 * {@link SomePropertiesToReference#SOME_STRING_PROPERTY_TO_REFERENCE}
	 * {@link SomePropertiesToReference#SOME_BOOLEAN_PROPERTY_TO_REFERENCE}
	 * {@link SomePropertiesToReference#SOME_ENUM_PROPERTY_TO_REFERENCE}
	 * <p>
	 * Defaults to {@link Defaults#SOME_YET_ANOTHER_PROPERTY_KEY} as link.
	 */
	public static final String SOME_YET_ANOTHER_PROPERTY_KEY = "some.yet.another.property.key";


	@HibernateSearchConfiguration(ignore = true)
	public static final String PROPERTY_KEY_TO_BE_IGNORED = "property.key.to.be.ignored";

	public static class Defaults {
		public static final String SOME_OTHER_PROPERTY_KEY = "default";
		public static final boolean SOME_YET_ANOTHER_PROPERTY_KEY = false;
	}
}
