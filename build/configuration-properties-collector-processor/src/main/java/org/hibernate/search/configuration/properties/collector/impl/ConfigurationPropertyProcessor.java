/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import static org.hibernate.search.configuration.properties.collector.impl.AnnotationUtils.isIgnored;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

import com.google.gson.Gson;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions({
		Configuration.PROPERTY_PREFIX,
		Configuration.JAVADOC_LINK,
		Configuration.IGNORE_PATTERN,
		Configuration.IGNORE_KEY_VALUE_PATTERN,
		Configuration.GENERATED_FILE_NAME
})
public class ConfigurationPropertyProcessor extends AbstractProcessor {

	private static final Predicate<Map.Entry<String, ConfigurationProperty>> API_FILTER = entry -> HibernateSearchConfiguration.Type.API.equals(
			entry.getValue().type() );
	private static final Predicate<Map.Entry<String, ConfigurationProperty>> SPI_FILTER = entry -> HibernateSearchConfiguration.Type.SPI.equals(
			entry.getValue().type() );

	private ConfigurationPropertyCollector propertyCollector;
	private String fileName;
	private Optional<Pattern> ignore;
	private final String javadocFolderName;

	public ConfigurationPropertyProcessor() {
		this( "apidocs" );
	}

	public ConfigurationPropertyProcessor(String javadocFolderName) {
		this.javadocFolderName = javadocFolderName;
	}


	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init( processingEnv );

		String pattern = processingEnv.getOptions().get( Configuration.IGNORE_PATTERN );
		this.ignore = Optional.ofNullable( pattern ).map( Pattern::compile );
		this.fileName = processingEnv.getOptions().getOrDefault( Configuration.GENERATED_FILE_NAME, "properties" );
		String configPrefix = processingEnv.getOptions().getOrDefault( Configuration.PROPERTY_PREFIX, "" );
		String javadocsBaseLink = processingEnv.getOptions().getOrDefault( Configuration.JAVADOC_LINK, "" );

		String keyPattern = processingEnv.getOptions().getOrDefault( Configuration.IGNORE_KEY_VALUE_PATTERN, ".*\\.$" );
		Pattern ignoreKeys = Pattern.compile( keyPattern );
		Path javadocs = locateJavaDocFolder();

		this.propertyCollector = new ConfigurationPropertyCollector( processingEnv, configPrefix.split( "," ), javadocs, ignoreKeys,
				javadocsBaseLink
		);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> rootElements = roundEnv.getRootElements();

		// first let's go through all root elements and see if we can find *Settings classes:
		for ( Element element : rootElements ) {
			if ( isSettingsClass( element ) ) {
				process( propertyCollector, element );
			}
		}

		// means we might have some inner classes that we also wanted to consider for config property processing
		// so let's see if we need to process any:
		for ( TypeElement annotation : annotations ) {
			if ( annotation.getQualifiedName().contentEquals( HibernateSearchConfiguration.class.getName() ) ) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith( annotation );
				for ( Element element : elements ) {
					if ( isTypeElement( element ) ) {
						process( propertyCollector, element );
					}
				}
			}
		}

		if ( roundEnv.processingOver() ) {
			beforeExit();
		}

		return true;
	}

	public Map<String, ConfigurationProperty> collectedProperties() {
		return propertyCollector.properties();
	}

	private void beforeExit() {
		if ( propertyCollector.hasProperties() ) {
			writeProperties( fileName + ".json", (map, w) -> new Gson().toJson( map, w ) );

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
		try ( Writer writer = processingEnv.getFiler()
				.createResource( StandardLocation.SOURCE_OUTPUT, "", fileName )
				.openWriter()
		) {
			propertyCollector.write( transformer, writer );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void process(ConfigurationPropertyCollector propertyCollector, Element element) {
		if ( !isIgnored( element ) && !ignore.map( p -> p.matcher( element.toString() ).matches() ).orElse( Boolean.FALSE ) ) {
			propertyCollector.visitType( (TypeElement) element );
		}
	}

	private boolean isSettingsClass(Element element) {
		return element.getKind().equals( ElementKind.CLASS ) &&
				element.getSimpleName().toString().endsWith( "Settings" );
	}

	private boolean isTypeElement(Element element) {
		return element instanceof TypeElement;
	}

	private Path locateJavaDocFolder() {
		try {
			Path parent = Path.of(
					processingEnv.getFiler()
							.createResource( StandardLocation.SOURCE_OUTPUT, "", "locationfinder" )
							.toUri()
			);
			while ( !"target".equals( parent.getFileName().toString() ) ) {
				parent = parent.getParent();
			}
			return parent.resolve( "site" ).resolve( javadocFolderName );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
