/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.hibernate.search.configuration.properties.collector.impl.Configuration;
import org.hibernate.search.configuration.properties.collector.impl.ConfigurationProperty;
import org.hibernate.search.configuration.properties.collector.impl.ConfigurationPropertyProcessor;
import org.hibernate.search.configuration.properties.collector.sources.IgnoredSampleConfigAnnotatedSettings;
import org.hibernate.search.configuration.properties.collector.sources.JustSomeClass;
import org.hibernate.search.configuration.properties.collector.sources.SampleConfigAnnotatedSettings;
import org.hibernate.search.configuration.properties.collector.sources.SampleConfigNotAnnotatedSettings;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationPropertiesProcessorTest {

	private final JavaCompiler compiler;
	private final Path target;
	private ConfigurationPropertyProcessor configurationPropertyProcessor;

	public ConfigurationPropertiesProcessorTest() {
		compiler = ToolProvider.getSystemJavaCompiler();
		target = Path.of(
				ConfigurationPropertiesProcessorTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()
		).getParent();
	}

	@Before
	public void setUp() {
		configurationPropertyProcessor = new ConfigurationPropertyProcessor( "testapidocs" );
	}

	@Test
	public void process() {
		compile(
				Optional.of( "hibernate.search." ),
				source( SampleConfigAnnotatedSettings.class ),
				source( SampleConfigNotAnnotatedSettings.class ),
				source( JustSomeClass.class ),
				source( IgnoredSampleConfigAnnotatedSettings.class )
		);

		assertThat( configurationPropertyProcessor.collectedProperties() )
				.hasSize( 4 )
				.containsOnlyKeys(
						SampleConfigAnnotatedSettings.class.getName() + "#" + "SOME_PROPERTY_KEY",
						SampleConfigNotAnnotatedSettings.class.getName() + "#" + "SOME_OTHER_PROPERTY_KEY",
						SampleConfigNotAnnotatedSettings.class.getName() + "#" + "SOME_YET_ANOTHER_PROPERTY_KEY",
						JustSomeClass.InnerConfigurationSettings.class.getName()
								.replace( "$", "." ) + "#" + "SOME_INNER_SETTING"
				);

		assertThat( configurationPropertyProcessor.collectedProperties().values() )
				.extracting( ConfigurationProperty::key )
				.map( ConfigurationProperty.Key::toString )
				.containsOnly(
						"replacement.prefix." + SampleConfigAnnotatedSettings.SOME_PROPERTY_KEY + "/" + "replacement.another.prefix." + SampleConfigAnnotatedSettings.SOME_PROPERTY_KEY,
						"hibernate.search." + SampleConfigNotAnnotatedSettings.SOME_OTHER_PROPERTY_KEY,
						"hibernate.search." + SampleConfigNotAnnotatedSettings.SOME_YET_ANOTHER_PROPERTY_KEY,
						"hibernate.search." + JustSomeClass.InnerConfigurationSettings.SOME_INNER_SETTING
				);
		assertThat( configurationPropertyProcessor.collectedProperties().values() )
				.extracting( ConfigurationProperty::defaultValue )
				.containsOnly(
						"default",
						"",
						"",
						false
				);
	}

	public Path source(Class<?> klass) {
		return target.getParent().resolve( "src/test/java" )
				.resolve( klass.getName().replace( ".", File.separator ) + ".java" );
	}

	public boolean compile(Optional<String> prefix, Path... sourceFiles) {
		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects( sourceFiles );

		try {
			fileManager.setLocationFromPaths(
					StandardLocation.CLASS_OUTPUT,
					Arrays.asList( Files.createDirectories( target.resolve( "processor-generated-test-classes" ) )
					)
			);
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		List<String> options = new ArrayList<>();
		prefix.ifPresent( p -> options.add(
				String.format(
						Locale.ROOT,
						"-A%s=%s",
						Configuration.PROPERTY_PREFIX,
						p
				)
		) );

		JavaCompiler.CompilationTask task = compiler.getTask(
				null,
				fileManager,
				null,
				options,
				null,
				compilationUnits
		);

		task.setProcessors( Arrays.asList( configurationPropertyProcessor ) );

		return task.call();
	}
}
