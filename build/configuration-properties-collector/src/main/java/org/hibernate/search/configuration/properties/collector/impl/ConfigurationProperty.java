/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfigurationProperty implements Comparable<ConfigurationProperty> {

	public enum Type {
		/**
		 * Configuration property type API/SPI will be determined by inspecting the package in which a class is located.
		 * In case package contains {@code spi} package at any upper levels the type will be {@code SPI}, otherwise - {@code API}
		 */
		API,
		SPI
	}

	private static final Comparator<ConfigurationProperty> CONFIGURATION_PROPERTY_COMPARATOR = Comparator.comparing(
			c -> c.key().key );
	private Key key;
	private String javadoc;
	private String sourceClass;

	private Type type;

	private Object defaultValue;

	private String anchorPrefix;
	private String moduleName;

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

	public Type type() {
		return type;
	}

	public ConfigurationProperty type(Type type) {
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

	public String anchorPrefix() {
		return anchorPrefix;
	}

	public ConfigurationProperty anchorPrefix(String anchorPrefix) {
		this.anchorPrefix = anchorPrefix.replaceAll( "[^\\w-.]", "_" );
		return this;
	}

	public String moduleName() {
		return moduleName;
	}

	public ConfigurationProperty moduleName(String moduleName) {
		this.moduleName = moduleName;
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
				", anchorPrefix='" + anchorPrefix + '\'' +
				", moduleName='" + moduleName + '\'' +
				'}';
	}

	@Override
	public int compareTo(ConfigurationProperty o) {
		return CONFIGURATION_PROPERTY_COMPARATOR.compare( this, o );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ConfigurationProperty that = (ConfigurationProperty) o;
		return Objects.equals( key, that.key )
				&& Objects.equals( javadoc, that.javadoc )
				&& Objects.equals( sourceClass, that.sourceClass )
				&& type == that.type
				&& Objects.equals( defaultValue, that.defaultValue )
				&& Objects.equals( anchorPrefix, that.anchorPrefix )
				&& Objects.equals( moduleName, that.moduleName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( key, javadoc, sourceClass, type, defaultValue, anchorPrefix, moduleName );
	}

	public static class Key {
		private final List<String> prefixes;
		private final String key;

		public Key(List<String> prefixes, String key) {
			this.key = key;
			this.prefixes = prefixes;
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
			if ( prefixes.isEmpty() ) {
				return key;
			}
			else {
				return prefixes.stream()
						.map( p -> p + key )
						.collect( Collectors.joining( "/" ) );
			}
		}
	}
}
