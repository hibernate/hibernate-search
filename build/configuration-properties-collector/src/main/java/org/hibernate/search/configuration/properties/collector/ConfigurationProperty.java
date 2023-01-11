/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

public class ConfigurationProperty {

	private Key key;
	private String javadoc;
	private String sourceClass;

	private HibernateSearchConfiguration.Type type;

	private Object defaultValue;

	public Key key() {
		return key;
	}

	public ConfigurationProperty key(Key key) {
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

	public static class Key {
		private final List<String> prefixes;
		private final String key;

		public Key(List<String> prefixes, String key) {
			this.key = key;
			this.prefixes = prefixes;
		}

		public void overridePrefixes(String... prefixes) {
			overridePrefixes( Arrays.asList( prefixes ) );
		}

		public void overridePrefixes(List<String> prefixes) {
			this.prefixes.clear();
			this.prefixes.addAll( prefixes );
		}

		public boolean matches(Pattern pattern) {
			return pattern.matcher( key ).matches();
		}

		public List<String> resolvedKeys() {
			if ( prefixes.isEmpty() ) {
				return Collections.singletonList( key );
			}
			else {
				return prefixes.stream()
						.map( p -> p + key )
						.collect( Collectors.toList() );
			}
		}

		@Override
		public String toString() {
			return toString( "/" );
		}

		public String toHtmlString() {
			return toString( "</br>" );
		}

		private String toString(String delimiter) {
			if ( prefixes.isEmpty() ) {
				return key;
			}
			else {
				return prefixes.stream()
						.map( p -> p + key )
						.collect( Collectors.joining( delimiter ) );
			}
		}
	}
}
