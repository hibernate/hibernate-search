/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.report.loggers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.yaml.snakeyaml.Yaml;

@SupportedAnnotationTypes("org.hibernate.search.util.common.logging.CategorizedLogger")
@SupportedOptions({ Configuration.MODULE_NAME })
public class LoggerCategoriesProcessor extends AbstractProcessor {

	private Messager messager;
	private final Map<String, String> categories = new TreeMap<>();
	private final Map<String, Set<String>> categoryLevels = new TreeMap<>();
	private String moduleName;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init( processingEnv );
		messager = processingEnv.getMessager();

		moduleName = processingEnv.getOptions().get( Configuration.MODULE_NAME );
		if ( moduleName == null || moduleName.isBlank() ) {
			throw new IllegalArgumentException(
					"Module name cannot be null nor blank. Specify the %s annotation processor argument to define the module name"
							.formatted( Configuration.MODULE_NAME ) );
		}
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for ( TypeElement annotation : annotations ) {
			Set<? extends Element> loggers = roundEnv.getElementsAnnotatedWith( annotation );
			for ( Element el : loggers ) {
				if ( el.getKind().isInterface() || el.getKind().isClass() ) {
					TypeElement logger = (TypeElement) el;
					if ( extendsBasicLogger( logger ) || hasLoggingMethods( logger ) ) {
						messager.printMessage( Diagnostic.Kind.NOTE, "Logger class to process: %s".formatted( logger ) );
						for ( AnnotationMirror mirror : logger.getAnnotationMirrors() ) {
							if ( processingEnv.getTypeUtils().isSameType( annotation.asType(), mirror.getAnnotationType() ) ) {
								String category = getAnnotationValueAsString( mirror, "category" );
								String description = getAnnotationValueAsString( mirror, "description" );

								if ( description != null && !description.isBlank() ) {
									if ( categories.put( category, description ) != null ) {
										messager.printMessage( Diagnostic.Kind.ERROR,
												"Logging category %sis already defined in this module. Failed on logger: %s"
														.formatted( category, logger ) );
									}
								}
								else {
									messager.printMessage( Diagnostic.Kind.WARNING,
											"Logger %s either has some log-message methods or extends a BasicLogger, but does not provide any description on what it is used for."
													.formatted( category ) );
								}

								categoryLevels.computeIfAbsent( category, k -> new TreeSet<>() )
										.addAll( loggingLevels( logger ) );
							}
						}
					}
				}
			}
		}

		if ( roundEnv.processingOver() ) {
			try {
				FileObject report = processingEnv.getFiler().createResource(
						StandardLocation.CLASS_OUTPUT, "", "META-INF/hibernate-search/logging-categories.yaml" );
				try ( Writer writer = new OutputStreamWriter( report.openOutputStream(), StandardCharsets.UTF_8 ) ) {
					writer.write( """
							# This is a Hibernate Search internal report file.
							# Do NOT rely on its presence and/or format.
							#
							# List of logging categories that this module utilizes.
							#
							""" );
					if ( categories.isEmpty() ) {
						writer.write( "# This Hibernate Search module does not use any logging categories.\n" );
					}
					else {
						Yaml yaml = new Yaml();
						yaml.dump(
								Map.of(
										ReportConstants.ROOT,
										Map.of(
												ReportConstants.MODULE_NAME, moduleName,
												ReportConstants.CATEGORIES, toYamlCategories( categories, categoryLevels )
										)
								),
								writer
						);
					}
				}
				catch (IOException e) {
					throw new RuntimeException( e );
				}
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}


		return false;
	}

	private List<Map<String, Object>> toYamlCategories(Map<String, String> categories, Map<String, Set<String>> levels) {
		List<Map<String, Object>> values = new ArrayList<>();
		for ( var entry : categories.entrySet() ) {
			Map<String, Object> value = new HashMap<>();
			value.put( ReportConstants.CATEGORY_NAME, entry.getKey() );
			value.put( ReportConstants.CATEGORY_DESCRIPTION, entry.getValue() );
			value.put( ReportConstants.LOG_LEVELS, new ArrayList<>( levels.getOrDefault( entry.getKey(), Set.of() ) ) );

			values.add( value );
		}
		return values;
	}

	private boolean hasLoggingMethods(TypeElement logger) {
		for ( Element element : processingEnv.getElementUtils().getAllMembers( logger ) ) {
			if ( element.getKind() == ElementKind.METHOD ) {
				ExecutableElement executable = (ExecutableElement) element;
				if ( isVoid( executable ) && hasLoggingAnnotation( executable ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> loggingLevels(TypeElement logger) {
		Set<String> levels = new TreeSet<>();
		for ( Element element : processingEnv.getElementUtils().getAllMembers( logger ) ) {
			if ( element.getKind() == ElementKind.METHOD ) {
				ExecutableElement executable = (ExecutableElement) element;
				Optional<AnnotationMirror> logMessage = getLogMessage( executable );

				logMessage
						.ifPresent( annotationMirror -> levels.add( getAnnotationValueAsString( annotationMirror, "level" ) ) );
			}
		}
		return levels;
	}

	private boolean hasLoggingAnnotation(ExecutableElement executable) {
		return getLogMessage( executable ).isPresent();
	}

	private Optional<AnnotationMirror> getLogMessage(ExecutableElement executable) {
		for ( AnnotationMirror am : executable.getAnnotationMirrors() ) {
			if ( ( (TypeElement) am.getAnnotationType().asElement() ).getQualifiedName()
					.contentEquals( "org.jboss.logging.annotations.LogMessage" ) ) {
				return Optional.of( am );
			}
		}
		return Optional.empty();
	}

	private boolean isVoid(ExecutableElement executable) {
		return executable.getReturnType().getKind() == TypeKind.VOID;
	}

	private boolean extendsBasicLogger(TypeElement logger) {
		List<? extends TypeMirror> interfaces = logger.getInterfaces();
		for ( TypeMirror anInterface : interfaces ) {
			Element el = processingEnv.getTypeUtils().asElement( anInterface );
			if ( ( (TypeElement) el ).getQualifiedName().contentEquals( "org.jboss.logging.BasicLogger" ) ) {
				return true;
			}
		}
		return false;
	}

	private String getAnnotationValueAsString(AnnotationMirror annotationMirror, String name) {
		AnnotationValue annotationValue = getAnnotationValue( annotationMirror, name );
		if ( annotationValue == null ) {
			return "";
		}
		return annotationValue.getValue().toString();
	}

	private AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String name) {
		var elementValues = annotationMirror.getElementValues();
		for ( var entry : elementValues.entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals( name ) ) {
				return entry.getValue();
			}
		}
		return null;
	}
}
