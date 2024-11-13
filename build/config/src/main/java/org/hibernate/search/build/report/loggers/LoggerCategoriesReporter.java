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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
					List<String> descr = (List<String>) category.get( ReportConstants.CATEGORY_DESCRIPTION );
					if ( descr != null ) {
						c.descriptions.addAll( descr );
					}
				}
			}
		}

		try ( FileOutputStream fos = new FileOutputStream( doc.toFile() );
				Writer writer = new OutputStreamWriter( fos, StandardCharsets.UTF_8 ) ) {
			for ( Category category : report.values() ) {
				writer.write( "[[logging-category-%s]]`%s`::\n".formatted( category.name.replace( '.', '-' ), category.name ) );
				if ( !category.descriptions.isEmpty() ) {
					writer.write( "Description:::\n" );
					for ( String description : category.descriptions ) {
						writer.write( "* %s\n".formatted( description ) );
					}
				}
				writer.write( "Used in modules:::\n" );
				for ( String module : category.modules ) {
					writer.write( "* `%s`\n".formatted( module ) );
				}
			}
		}
	}

	private static class Category {
		String name;
		Set<String> descriptions = new TreeSet<>();
		Set<String> modules = new TreeSet<>();

		public Category(String name) {
			this.name = name;
		}
	}
}
