/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector;

import static org.hibernate.search.configuration.properties.collector.utils.AnnotationUtils.findAnnotation;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import org.hibernate.search.configuration.properties.collector.utils.AnnotationUtils;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ConfigurationPropertyCollector {

	// assume that spi/impl/internal packages are not for public use and consider all of them as SPI:
	private static final Pattern SPI_PATTERN = Pattern.compile(
			"(.*\\.spi$)|(.*\\.spi\\..*)|(.*\\.impl$)|(.*\\.impl\\..*)|(.*\\.internal$)|(.*\\.internal\\..*)" );

	private final Set<Name> processedTypes = new HashSet<>();
	private final Map<String, ConfigurationProperty> properties = new LinkedHashMap<>();
	private final Elements elementUtils;
	private final String configPrefix;
	private final Path javadocsLocation;
	private final String javadocsBaseLink;
	private final Pattern ignoreKeys;
	private final Messager messager;

	public ConfigurationPropertyCollector(ProcessingEnvironment processingEnvironment, String configPrefix,
			Path javadocsLocation, Pattern ignoreKeys,
			String javadocsBaseLink) {
		this.elementUtils = processingEnvironment.getElementUtils();
		this.configPrefix = configPrefix;
		this.javadocsLocation = javadocsLocation;
		this.javadocsBaseLink = javadocsBaseLink;
		this.ignoreKeys = ignoreKeys;
		this.messager = processingEnvironment.getMessager();
	}

	public void visitType(TypeElement element) {
		Name qualifiedName = element.getQualifiedName();
		if ( !processedTypes.contains( qualifiedName ) ) {
			processedTypes.add( qualifiedName );

			Optional<String> classPrefix = findAnnotation( element, HibernateSearchConfiguration.class )
					.flatMap(
							a -> a.attribute( "prefix", String.class )
									.map( ConfigurationPropertyCollector::nullIfBlank )
					);

			for ( Element inner : elementUtils.getAllMembers( element ) ) {
				if ( inner.getKind().equals( ElementKind.FIELD ) && inner instanceof VariableElement ) {
					processConstant( ( (VariableElement) inner ), classPrefix );
				}
			}
		}
	}

	public void write(BiConsumer<Map<String, ConfigurationProperty>, Writer> transformer, Writer writer) {
		transformer.accept( this.properties, writer );
	}

	public Map<String, ConfigurationProperty> properties() {
		return Collections.unmodifiableMap( properties );
	}

	private void processConstant(VariableElement constant, Optional<String> classPrefix) {
		Optional<AnnotationUtils.AnnotationAttributeHolder> annotation = findAnnotation( constant, HibernateSearchConfiguration.class );
		if ( annotation.flatMap( a -> a.attribute( "ignore", Boolean.class ) ).orElse( false ) ) {
			return;
		}

		String key = extractKey(
				constant,
				classPrefix,
				annotation.flatMap( a -> a.attribute( "prefix", String.class ) )
						.map( ConfigurationPropertyCollector::nullIfBlank )
		);
		if ( !ignoreKeys.matcher( key ).matches() ) {
			// Try to find a default value. Assumption is that the settings class has an inner class called "Defaults" and
			// the key for the default value is exactly the same as the config constant name:
			Object value = findDefault( constant );

			properties.put(
					constant.getEnclosingElement().toString() + "#" + constant.getSimpleName().toString(),
					new ConfigurationProperty()
							.javadoc( extractJavadoc( constant ) )
							.key( key )
							.sourceClass( constant.getEnclosingElement().toString() )
							.type( extractType( constant ) )
							.defaultValue( value )
			);
		}
	}


	private String extractKey(VariableElement constant, Optional<String> classPrefix, Optional<String> constantPrefix) {
		String prefix;
		if ( constantPrefix.isPresent() ) {
			prefix = constantPrefix.get();
		}
		else if ( classPrefix.isPresent() ) {
			prefix = classPrefix.get();
		}
		else {
			prefix = configPrefix;
		}

		return prefix + Objects.toString( constant.getConstantValue(), "NOT_FOUND#" + constant.getSimpleName() );
	}

	private HibernateSearchConfiguration.Type extractType(VariableElement constant) {
		String packageName = constant.getEnclosingElement().getEnclosingElement().toString();
		return SPI_PATTERN.matcher( packageName ).matches() ?
				HibernateSearchConfiguration.Type.SPI :
				HibernateSearchConfiguration.Type.API;
	}

	private String extractJavadoc(VariableElement constant) {
		try {
			Element enclosingClass = constant.getEnclosingElement();
			Path docs = javadocsLocation.resolve(
					enclosingClass.toString().replace( ".", File.separator ) + ".html"
			);

			// calling getEnclosingElement() on class should return a package:
			String packagePath = enclosingClass.getEnclosingElement().toString().replace( ".", File.separator );

			Document javadoc = Jsoup.parse( docs.toFile() );

			org.jsoup.select.Elements blocks = javadoc.select( "#" + constant.getSimpleName() + " .block" );
			if ( !blocks.isEmpty() ) {
				org.jsoup.nodes.Element block = blocks.get( 0 );
				for ( org.jsoup.nodes.Element link : block.getElementsByTag( "a" ) ) {
					// only update links if they are not external:
					if ( !link.hasClass( "external-link" ) ) {
						link.attr( "href", javadocsBaseLink + packagePath + "/" + link.attr( "href" ) );
					}
				}
				return block.toString();
			}
			else {
				return elementUtils.getDocComment( constant );
			}
		}
		catch (IOException e) {
			messager.printMessage( Diagnostic.Kind.NOTE, "Wasn't able to find rendered javadocs for " + constant + ". Trying to read plain javadoc comment." );
			return elementUtils.getDocComment( constant );
		}
	}

	/**
	 * This really works only for string/primitive constants ... other types would just get null returned.
	 */
	private Object findDefault(VariableElement constant) {
		if ( constant.getEnclosingElement() instanceof TypeElement ) {
			for ( Element element : elementUtils.getAllMembers( (TypeElement) constant.getEnclosingElement() ) ) {
				if ( ElementKind.CLASS.equals( element.getKind() )
						&& element.getSimpleName().contentEquals( "Defaults" ) ) {
					for ( Element enclosedElement : element.getEnclosedElements() ) {
						if ( enclosedElement.getSimpleName().equals( constant.getSimpleName() ) ) {
							return ( (VariableElement) enclosedElement ).getConstantValue();
						}
					}
				}
			}
		}
		return null;
	}

	private static String nullIfBlank(String str) {
		return str != null && str.isBlank() ? null : str;
	}

}
