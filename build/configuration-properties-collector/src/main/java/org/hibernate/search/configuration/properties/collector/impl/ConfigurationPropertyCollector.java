/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import static org.hibernate.search.configuration.properties.collector.impl.ConfigurationRules.isClassIgnored;
import static org.hibernate.search.configuration.properties.collector.impl.ConfigurationRules.isConstantIgnored;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ConfigurationPropertyCollector {

	// assume that spi/impl/internal packages are not for public use and consider all of them as SPI:
	private static final Pattern SPI_PATTERN = Pattern.compile(
			"(.*\\.spi$)|(.*\\.spi\\..*)|(.*\\.impl$)|(.*\\.impl\\..*)|(.*\\.internal$)|(.*\\.internal\\..*)" );

	private final Map<String, ConfigurationProperty> properties = new TreeMap<>();
	private final Path javadocsLocation;
	private final String javadocsBaseLink;
	private final String anchor;
	private final String moduleName;

	public ConfigurationPropertyCollector(String javadocsBaseLink, Path javadocsLocation, String anchor,
			String moduleName) {
		this.javadocsLocation = javadocsLocation;
		this.javadocsBaseLink = javadocsBaseLink;
		this.anchor = anchor;
		this.moduleName = moduleName;
	}

	public void process() {
		locateConstants().ifPresent( this::processClasses );
	}

	private void processClasses(Document constants) {
		for ( Element table : constants.select( ".block-list li" ) ) {
			String className = table.selectFirst( ".caption" ).text();
			if ( !isClassIgnored( className ) ) {
				// assume that such class is a config class and we want to collect properties from it.
				Optional<Document> javadoc = obtainJavadoc( className );
				javadoc.ifPresent( doc -> {
					Iterator<Element> div = table.selectFirst( ".summary-table.three-column-summary" ).children()
							.iterator();

					// skip the header:
					div.next();
					div.next();
					div.next();

					// go through constants:
					while ( div.hasNext() ) {
						div.next();// ignore type
						Element constant = div.next();
						String value = stripQuotes( div.next().text() );
						String constantText = constant.text();
						if ( !isConstantIgnored( className, constantText, value ) ) {
							properties.put(
									className + "#" + constantText,
									new ConfigurationProperty()
											.key(
													new ConfigurationProperty.Key(
															ConfigurationRules.prefixes(
																	className, constantText, value ),
															value
													)
											)
											.javadoc(
													extractJavadoc(
															doc,
															className,
															constantText
													)
											)
											.defaultValue( findDefault( constants, className, constantText ) )
											.sourceClass( className )
											.anchorPrefix( anchor )
											.moduleName( moduleName )
											.type( extractType( className, constantText ) )
							);
						}
					}
				} );
			}
		}
	}

	private String withoutPackagePrefix(String className) {
		return className.substring( className.lastIndexOf( '.' ) + 1 );
	}

	private String packagePrefix(String className) {
		return className.substring( 0, className.lastIndexOf( '.' ) );
	}

	private String stripQuotes(String value) {
		if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
			return value.substring( 1, value.length() - 1 );
		}
		return value;
	}


	private Optional<Document> obtainJavadoc(String enclosingClass) {
		try {
			Path docs = javadocsLocation.resolve(
					enclosingClass.replace( ".", File.separator ) + ".html"
			);

			return Optional.of( Jsoup.parse( docs.toFile() ) );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to access javadocs for " + enclosingClass, e );
		}
	}

	private Optional<Document> locateConstants() {
		try {
			Path docs = javadocsLocation.resolve( "constant-values.html" );

			return Optional.of( Jsoup.parse( docs.toFile() ) );
		}
		catch (IOException e) {
			return Optional.empty();
		}
	}

	public void write(BiConsumer<Map<String, ConfigurationProperty>, Writer> transformer, Writer writer) {
		transformer.accept( this.properties, writer );
	}

	public Map<String, ConfigurationProperty> properties() {
		return Collections.unmodifiableMap( properties );
	}

	public boolean hasProperties() {
		return !properties.isEmpty();
	}

	public boolean hasProperties(Predicate<Map.Entry<String, ConfigurationProperty>> filter) {
		return properties.entrySet().stream().anyMatch( filter );
	}

	private ConfigurationProperty.Type extractType(String className, String constant) {
		String packageName = packagePrefix( className );
		return SPI_PATTERN.matcher( packageName ).matches() ?
				ConfigurationProperty.Type.SPI :
				ConfigurationProperty.Type.API;
	}

	private String extractJavadoc(Document javadoc, String className, String constant) {
		Elements blocks = javadoc.selectFirst( "#" + constant + " .member-signature" ).nextElementSiblings();
		if ( !blocks.isEmpty() ) {
			Elements result = new Elements();
			for ( Element block : blocks ) {
				if ( block.hasClass( "notes" ) ) {
					continue;
				}
				if ( block.hasClass( "deprecation-block" ) ) {
					block.attr(
							"style",
							"border-style:solid; border-width:thin;  border-radius:10px; padding:10px; margin-bottom:10px; margin-right:10px; display:inline-block;"
					);
				}
				for ( Element link : block.getElementsByTag( "a" ) ) {
					updateLink( className, link );
				}
				result.add( block );
			}
			return result.toString();
		}
		return "";
	}

	private void updateLink(String className, Element link) {
		String href = link.attr( "href" );
		// only update links if they are not external:
		if ( !link.hasClass( "external-link" ) ) {
			if ( href.startsWith( "#" ) ) {
				href = withoutPackagePrefix( className ) + ".html" + href;
			}
			String packagePath = packagePrefix( className ).replace( ".", File.separator );
			href = javadocsBaseLink + packagePath + "/" + href;
		}
		else if ( href.contains( "/build/parents/" ) && href.contains( "/apidocs" ) ) {
			// means a link was to a class from other module and javadoc plugin generated some external link
			// that won't work. So we replace it:
			href = javadocsBaseLink + href.substring( href.indexOf( "/apidocs" ) + "/apidocs".length() );
		}
		link.attr( "href", href );
	}

	/**
	 * This really works only for string/primitive constants ... other types are not present in javadocs and would just get null returned.
	 */
	private String findDefault(Document constants, String className, String constant) {
		// 1. find corresponding table with defaults:
		return constants.select( ".caption" ).stream()
				.filter( e -> e.text().equals( className + ".Defaults" ) )
				.findAny()
				.flatMap(
						// 2. if we found a table - select constants and find if the one we need is present:
						tableHeader -> tableHeader.siblingElements().select( ".col-second" ).stream()
								.filter( e -> e.text().equals( constant ) )
								.findAny().map(
										constantField -> constantField.nextElementSibling().text()
								)
				).orElse( null );
	}
}
