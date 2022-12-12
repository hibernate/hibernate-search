/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

public class ConfigurationProperty {

	private String key;
	private String javadoc;
	private String sourceClass;

	private HibernateSearchConfiguration.Type type;

	private Object defaultValue;

	public String key() {
		return key;
	}

	public ConfigurationProperty key(String key) {
		this.key = key;
		return this;
	}

	public String javadoc() {
		return javadoc;
	}

	public ConfigurationProperty javadoc(String javadoc) {
		this.javadoc = javadoc == null ? "" : javadoc;
		return this;
	}

	public String sourceClass() {
		return sourceClass;
	}

	public ConfigurationProperty sourceClass(String sourceClass) {
		this.sourceClass = sourceClass;
		return this;
	}

	public HibernateSearchConfiguration.Type type() {
		return type;
	}

	public ConfigurationProperty type(HibernateSearchConfiguration.Type type) {
		this.type = type;
		return this;
	}

	public Object defaultValue() {
		return defaultValue;
	}

	public ConfigurationProperty defaultValue(Object defaultValue) {
		this.defaultValue = defaultValue == null ? "" : defaultValue;
		return this;
	}

	@Override
	public String toString() {
		return "ConfigurationProperty{" +
				"key='" + key + '\'' +
				", javadoc='" + javadoc + '\'' +
				", sourceClass='" + sourceClass + '\'' +
				", type='" + type + '\'' +
				", default='" + defaultValue + '\'' +
				'}';
	}
}
