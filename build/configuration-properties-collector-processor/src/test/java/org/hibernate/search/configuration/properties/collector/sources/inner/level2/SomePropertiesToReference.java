/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.sources.inner.level2;

public class SomePropertiesToReference {

	private SomePropertiesToReference() {
	}

	/**
	 * value
	 */
	public static final String SOME_STRING_PROPERTY_TO_REFERENCE = "value";
	/**
	 * true
	 */
	public static final Boolean SOME_BOOLEAN_PROPERTY_TO_REFERENCE = Boolean.TRUE;
	/***
	 * value1
	 */
	public static final SomeEnum SOME_ENUM_PROPERTY_TO_REFERENCE = SomeEnum.VALUE1;

	enum SomeEnum {
		VALUE1, VALUE2
	}

}
