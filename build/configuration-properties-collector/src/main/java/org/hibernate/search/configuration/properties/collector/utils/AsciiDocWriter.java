/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.utils;


import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.search.configuration.properties.collector.ConfigurationProperty;

public class AsciiDocWriter implements BiConsumer<Map<String, ConfigurationProperty>, Writer> {

	private final Optional<String> moduleName;

	public AsciiDocWriter(String moduleName) {
		this.moduleName = Optional.ofNullable( moduleName );
	}

	@Override
	public void accept(Map<String, ConfigurationProperty> propertyMap, Writer writer) {
		try {
			moduleName.ifPresent( name -> tryToWriteLine( "= ", name, writer ) );
			moduleName.ifPresent( name -> tryToWriteLine( ".", name, writer ) );
			writer.write( "|===\n" );
			writer.write( "|Configuration key |Description |Configuration type |Default value" );
			moduleName.ifPresent( ignore -> tryToWriteLine( " |", "Module", writer ) );
			writer.write( "\n" );
			writer.write( "\n" );
			for ( Map.Entry<String, ConfigurationProperty> entry : propertyMap.entrySet() ) {
				writer.write( "|" );
				writer.write( entry.getValue().key() );
				// using inline passthrough for javadocs to not render HTML.
				writer.write( " | +++ " );
				writer.write( entry.getValue().javadoc() );
				writer.write( " +++ |" );
				writer.write( entry.getValue().type().toString() );
				writer.write( " |" );
				writer.write( Objects.toString( entry.getValue().defaultValue() ) );
				moduleName.ifPresent( name -> tryToWriteLine( " |", name, writer ) );
				writer.write( "\n" );
			}
			writer.write( "|===\n" );
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
