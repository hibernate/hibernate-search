/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.utils;


import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.configuration.properties.collector.ConfigurationProperty;

public class AsciiDocWriter implements BiConsumer<Map<String, ConfigurationProperty>, Writer> {

	private final Optional<String> moduleName;
	private final Predicate<Map.Entry<String, ConfigurationProperty>> filter;

	public AsciiDocWriter(String moduleName, Predicate<Map.Entry<String, ConfigurationProperty>> filter) {
		this.moduleName = Optional.ofNullable( moduleName );
		this.filter = filter;
	}

	@Override
	public void accept(Map<String, ConfigurationProperty> propertyMap, Writer writer) {
		List<Map.Entry<String, ConfigurationProperty>> entries = propertyMap.entrySet().stream()
				.filter( filter )
				.collect( Collectors.toList() );
		if ( entries.isEmpty() ) {
			// nothing to write - return fast.
			return;
		}
		try {
			moduleName.ifPresent( name -> tryToWriteLine( "== ", name, writer ) );
			writer.write( '\n' );
			for ( Map.Entry<String, ConfigurationProperty> entry : entries ) {
				Iterator<String> keys = entry.getValue().key().resolvedKeys().iterator();
				writer.write( '`' );
				writer.write( keys.next() );
				writer.write( '`' );
				writer.write( "::\n" );

				boolean hasMultipleKeys = false;
				if ( keys.hasNext() ) {
					hasMultipleKeys = true;
					writer.write( "Other variants: " );
				}
				while ( keys.hasNext() ) {
					writer.write( '`' );
					writer.write( keys.next() );
					writer.write( '`' );
					if ( keys.hasNext() ) {
						writer.write( ", " );
					}
				}

				if ( hasMultipleKeys ) {
					writer.write( "\n+\n" );
				}

				// using inline passthrough for javadocs to not render HTML.
				writer.write( "+++ " );
				writer.write( entry.getValue().javadoc() );
				writer.write( " +++ " );

				String defaultValue = Objects.toString( entry.getValue().defaultValue(), "" );
				if ( !defaultValue.isBlank() ) {
					writer.write( "\n+\n" );
					writer.write( "Default value: `" );
					writer.write( defaultValue );
					writer.write( '`' );
				}

				writer.write( '\n' );
			}
			writer.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void tryToWriteLine(String prefix, String value, Writer writer) {
		try {
			writer.write( prefix );
			writer.write( value );
			writer.write( "\n" );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
