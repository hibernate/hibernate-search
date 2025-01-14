/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.impl;

import static org.hibernate.search.metamodel.processor.impl.ProcessorElementUtils.flattenedAnnotations;
import static org.hibernate.search.metamodel.processor.impl.ProcessorElementUtils.propertyElements;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.metamodel.processor.annotation.processing.impl.ProcessorAnnotationProcessorContext;
import org.hibernate.search.metamodel.processor.annotation.processing.impl.ProcessorPropertyMappingAnnotationProcessor;
import org.hibernate.search.metamodel.processor.annotation.processing.impl.ProcessorTypeMappingAnnotationProcessor;
import org.hibernate.search.metamodel.processor.mapping.impl.ProcessorIntrospectorContext;
import org.hibernate.search.metamodel.processor.mapping.impl.ProcessorPojoModelsBootstrapIntrospector;
import org.hibernate.search.metamodel.processor.writer.impl.MetamodelClassWriter;
import org.hibernate.search.metamodel.processor.writer.impl.MetamodelNamesFormatter;

public class IndexedEntityMetamodelAnnotationProcessor implements MetamodelAnnotationProcessor {

	private static final String ANNOTATION_INDEXED = "org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed";
	private final HibernateSearchMetamodelProcessorContext context;
	private final ProcessorIntrospectorContext introspectorContext;
	private ProcessorPojoModelsBootstrapIntrospector introspector;

	public IndexedEntityMetamodelAnnotationProcessor(HibernateSearchMetamodelProcessorContext context) {
		this.context = context;
		this.introspectorContext = new ProcessorIntrospectorContext( context );
	}

	@Override
	public void process(RoundEnvironment roundEnv) {
		TypeElement indexedAnnotation = context.elementUtils().getTypeElement( ANNOTATION_INDEXED );
		Set<? extends Element> indexedEntities = roundEnv.getElementsAnnotatedWith( indexedAnnotation );

		try ( CloseableSearchMapping searchMapping = StandalonePojoIntegrationBooter.builder()
				.introspectorCustomizer( this::wrapIntrospector )
				.property( "hibernate.search.backend.directory.type", "local-heap" )
				.property(
						StandalonePojoMapperSettings.MAPPING_CONFIGURER,
						BeanReference.ofInstance( (StandalonePojoMappingConfigurer) configurationContext -> {
							// disable any discovery by annotations -- that won't work as we do not implement annotations in the pojo model
							configurationContext.annotationMapping()
									.discoverAnnotatedTypesFromRootMappingAnnotations( false )
									.discoverAnnotationsFromReferencedTypes( false )
									.discoverJandexIndexesFromAddedTypes( false )
									.buildMissingDiscoveredJandexIndexes( false );

							ProgrammaticMappingConfigurationContext programmaticMapping =
									configurationContext.programmaticMapping();

							ProcessorAnnotationProcessorContext ctx = new ProcessorAnnotationProcessorContext(
									context.elementUtils(),
									context.typeUtils(), context.messager(), programmaticMapping
							);
							introspectorContext.processorAnnotationProcessorContext( ctx );

							for ( Element el : indexedEntities ) {
								if ( el.getKind().isClass() || el.getKind().isInterface() ) {
									TypeElement indexedEntityType = (TypeElement) el;

									String typeName = indexedEntityType.getQualifiedName().toString();

									introspectorContext.typeElementsByName( typeName, indexedEntityType );

									TypeMappingStep typeMappingContext = programmaticMapping.type( typeName );
									typeMappingContext.indexed().enabled( true );
									typeMappingContext.searchEntity().name( typeName );

									processTypeAndProperties( indexedEntityType, typeMappingContext, ctx );
								}
								else {
									// TODO: generate message bundle with JBoss Logging ?
									context.messager()
											.printMessage(
													Diagnostic.Kind.ERROR,
													"Unexpected location of the " + indexedAnnotation.getQualifiedName()
															+ ". Expected to be placed on an indexed type.",
													el
											);
								}
							}

						} )
				).build()
				.boot() ) {

			for ( SearchIndexedEntity<?> entity : searchMapping.allIndexedEntities() ) {
				context.messager().printMessage( Diagnostic.Kind.NOTE, entity.name() );
				String packageName = entity.name().substring( 0, entity.name().lastIndexOf( "." ) );

				MetamodelClassWriter.Builder builder =
						new MetamodelClassWriter.Builder( MetamodelNamesFormatter.DEFAULT, packageName, entity.name() );

				entity.indexManager().descriptor().staticFields()
						.forEach( f -> {
							if ( f.parent().isRoot() ) {
								if ( f.isValueField() ) {
									IndexValueFieldDescriptor valueField = f.toValueField();
									builder.addProperty( valueField );
								}
								else {
									IndexObjectFieldDescriptor objectField = f.toObjectField();
									builder.addProperty( objectField );
								}
							}
						} );
				try {
					JavaFileObject source = context.filer().createSourceFile( builder.fqcn() );
					context.messager().printMessage( Diagnostic.Kind.NOTE, source.toUri().toString() );
					try ( Writer writer = source.openWriter() ) {
						writer.append( builder.formatted() );
					}
					catch (IOException e) {
						throw new RuntimeException( e );
					}
				}
				catch (IOException e) {
					throw new RuntimeException( e );
				}
			}
		}

		context.messager().printMessage( Diagnostic.Kind.NOTE, "End" );
	}

	public static void processTypeAndProperties(TypeElement typeElement, TypeMappingStep typeMappingContext,
			ProcessorAnnotationProcessorContext ctx) {
		if ( !ctx.processedTypes().add( typeElement ) ) {
			return;
		}

		flattenedAnnotations( ctx.types(), typeElement )
				.forEach( annotationMirror -> {
					ProcessorTypeMappingAnnotationProcessor.processor( annotationMirror )
							.ifPresent( p -> p.process(
									typeMappingContext,
									annotationMirror,
									typeElement,
									ctx
							) );
				} );

		AtomicReference<PropertyMappingStep> documentId = new AtomicReference<>();
		AtomicReference<PropertyMappingStep> ormId = new AtomicReference<>();
		propertyElements( ctx.elements(), typeElement )
				.forEach( element -> {
					PropertyMappingStep step = typeMappingContext.property( element.getSimpleName().toString() );

					flattenedAnnotations( ctx.types(), element )
							.forEach( annotationMirror -> {
								if ( ProcessorPropertyMappingAnnotationProcessor.documentId( annotationMirror ) ) {
									documentId.set( step );
								}
								else if ( ProcessorPropertyMappingAnnotationProcessor.ormId( annotationMirror ) ) {
									ormId.set( step );
								}
								else {
									ProcessorPropertyMappingAnnotationProcessor.processor( annotationMirror )
											.ifPresent( p -> p.process(
													step,
													annotationMirror,
													element,
													ctx
											) );
								}
							} );
				} );
		if ( documentId.get() != null ) {
			documentId.get().documentId();
		}
		if ( ormId.get() != null ) {
			ormId.get().documentId();
		}
	}

	private ProcessorPojoModelsBootstrapIntrospector wrapIntrospector(PojoBootstrapIntrospector introspector) {
		this.introspector = new ProcessorPojoModelsBootstrapIntrospector( introspectorContext, introspector );
		return this.introspector;
	}
}
