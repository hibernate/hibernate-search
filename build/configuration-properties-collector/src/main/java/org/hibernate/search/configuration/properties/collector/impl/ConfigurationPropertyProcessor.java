/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ConfigurationPropertyProcessor implements AutoCloseable {

	private static final Predicate<Map.Entry<String, ConfigurationProperty>> API_FILTER = entry -> ConfigurationProperty.Type.API.equals(
			entry.getValue().type() );
	private static final Predicate<Map.Entry<String, ConfigurationProperty>> SPI_FILTER = entry -> ConfigurationProperty.Type.SPI.equals(
			entry.getValue().type() );

	private final ConfigurationPropertyCollector propertyCollector;
	private final String fileName;
	private final String javadocFolderName;
	private final Path target;
	private final Path output;

	public ConfigurationPropertyProcessor(String javadocFolderName, String javadocsBaseLink, Path target, Path output,
			String artifact, String moduleName) {
		this.javadocFolderName = javadocFolderName;
		this.target = target;
		this.output = output;
		this.fileName = artifact;

		this.propertyCollector = new ConfigurationPropertyCollector( javadocsBaseLink, locateJavaDocFolder(), artifact,
				moduleName
		);
	}


	public static void main(String[] args) {
		String javadocsBaseLink = args[0];
		Path target = new File( args[1] ).toPath();
		Path output = new File( args[2] ).toPath();
		String artifact = args[3];
		String moduleName = args[4];

		// we don't want to run this processor on parent poms so if that's what we got - return fast:
		if ( artifact.startsWith( "hibernate-search-parent-public" ) ) {
			return;
		}

		try ( ConfigurationPropertyProcessor processor = new ConfigurationPropertyProcessor(
				"apidocs",
				javadocsBaseLink,
				target,
				output,
				artifact,
				moduleName
		) ) {
			processor.process();
		}
	}

	public boolean process() {
		this.propertyCollector.process();

		return true;
	}

	@Override
	public void close() {
		if ( propertyCollector.hasProperties() ) {
			if ( propertyCollector.hasProperties( API_FILTER ) ) {
				writeProperties(
						fileName + ".asciidoc",
						new AsciiDocWriter(
								API_FILTER
						)
				);
			}
			if ( propertyCollector.hasProperties( SPI_FILTER ) ) {
				writeProperties(
						fileName + "-spi.asciidoc",
						new AsciiDocWriter(
								SPI_FILTER
						)
				);
			}
		}
	}

	private void writeProperties(String fileName, BiConsumer<Map<String, ConfigurationProperty>, Writer> transformer) {
		try ( Writer writer = new OutputStreamWriter( new FileOutputStream( Files.createDirectories( output )
				.resolve( fileName ).toFile() ), StandardCharsets.UTF_8 ) ) {
			propertyCollector.write( transformer, writer );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private Path locateJavaDocFolder() {
		Path site = target.resolve( "site" ).resolve( javadocFolderName );
		if ( !Files.exists( site ) ) {
			throw new IllegalStateException(
					"Was unable to locate javadocs. No processing is possible. Make sure that " +
							"the Javadocs are generated prior to running this processor." );
		}
		return site;
	}
}
