/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.report.loggers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

public class LoggerCategoriesReporter {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if ( args.length != 1 ) {
			throw new IllegalArgumentException( "Has to pass exactly one parameter specifying the report output directory!" );
		}
		Path doc = Path.of( args[0] ).resolve( "_hibernate-logging-categories.adoc" );
		Files.createDirectories( doc.getParent() );

		System.out.printf( Locale.ROOT, "Generating the Logging Categories report into %s\n", doc );

		Enumeration<URL> reports =
				LoggerCategoriesReporter.class.getClassLoader()
						.getResources( "META-INF/hibernate-search/logging-categories.yaml" );

		if ( reports.hasMoreElements() ) {
			System.out.printf( Locale.ROOT, "Found some logging categories yaml reports.\n" );
		}
		else {
			System.out.printf( Locale.ROOT, "Found *NO* logging categories yaml reports.\n" );
		}

		Map<String, Category> report = new TreeMap<>();

		while ( reports.hasMoreElements() ) {
			URL reportUrl = reports.nextElement();
			System.out.printf( "Processing report file: %s\n", reportUrl );
			try ( InputStreamReader reader =
					new InputStreamReader( reportUrl.openStream(), StandardCharsets.UTF_8 ) ) {
				Yaml yaml = new Yaml();
				Map<String, Object> load = yaml.load( reader );

				if ( load == null ) {
					System.err.printf( "Warning: report %s is empty or invalid\n", reportUrl );
					continue;
				}

				Map<String, Object> root = (Map<String, Object>) load.get( ReportConstants.ROOT );
				String moduleName = (String) root.get( ReportConstants.MODULE_NAME );
				List<Map<String, Object>> categories =
						(List<Map<String, Object>>) root.get( ReportConstants.CATEGORIES );

				for ( var category : categories ) {
					String name = (String) category.get( ReportConstants.CATEGORY_NAME );
					Category c = report.computeIfAbsent( name, Category::new );
					c.modules.add( moduleName );
					c.description = Objects.toString( category.get( ReportConstants.CATEGORY_DESCRIPTION ) );

					List<String> levels = (List<String>) category.get( ReportConstants.LOG_LEVELS );
					if ( levels != null ) {
						c.levels.addAll( levels );
					}
				}
			}
		}

		try ( FileOutputStream fos = new FileOutputStream( doc.toFile() );
				Writer writer = new OutputStreamWriter( fos, StandardCharsets.UTF_8 ) ) {
			for ( Category category : report.values() ) {
				writer.write( "[[logging-category-%s]]`%s`::\n".formatted( category.name.replace( '.', '-' ), category.name ) );
				writer.write( "Description:::\n" );
				writer.write( "%s\n".formatted( category.description ) );
				writer.write( "Used in modules:::\n" );
				writer.write( category.modules.stream().map( "`%s`"::formatted ).collect( Collectors.joining( ", " ) ) );
				writer.write( "\n" );
				if ( !category.levels.isEmpty() ) {
					writer.write( "Produces messages with log levels:::\n" );
					writer.write( category.levels.stream().map( "`%s`"::formatted ).collect( Collectors.joining( ", " ) ) );
					writer.write( "\n" );
				}
			}
		}
	}

	private static class Category {
		String name;
		String description;
		Set<String> modules = new TreeSet<>();
		Set<String> levels = new TreeSet<>();

		public Category(String name) {
			this.name = name;
		}
	}
}
