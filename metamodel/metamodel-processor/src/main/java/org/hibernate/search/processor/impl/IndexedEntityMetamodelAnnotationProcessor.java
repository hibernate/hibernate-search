/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.impl;

import static org.hibernate.search.processor.impl.ProcessorElementUtils.flattenedAnnotations;
import static org.hibernate.search.processor.impl.ProcessorElementUtils.propertyElements;
import static org.hibernate.search.processor.impl.ProcessorElementUtils.propertyName;

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
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.DefaultIdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.processor.annotation.processing.impl.ProcessorAnnotationProcessorContext;
import org.hibernate.search.processor.annotation.processing.impl.ProcessorPropertyMappingAnnotationProcessor;
import org.hibernate.search.processor.annotation.processing.impl.ProcessorTypeMappingAnnotationProcessor;
import org.hibernate.search.processor.mapping.impl.ProcessorIntrospectorContext;
import org.hibernate.search.processor.mapping.impl.ProcessorPojoModelsBootstrapIntrospector;
import org.hibernate.search.processor.model.impl.HibernateSearchProcessorEnum;
import org.hibernate.search.processor.writer.impl.MetamodelClassWriter;
import org.hibernate.search.processor.writer.impl.MetamodelNamesFormatter;
import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

public class IndexedEntityMetamodelAnnotationProcessor implements MetamodelAnnotationProcessor {

	private static final String ANNOTATION_INDEXED = "org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed";
	private final HibernateSearchMetamodelProcessorContext context;
	private final ProcessorIntrospectorContext introspectorContext;

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
				.property( "hibernate.search.backend.version_check.enabled", "false" )
				.property( "hibernate.search.schema_management.strategy", "none" )
				.property( "hibernate.search.backend.version", context.configuration().backendVersion() )
				.property( "hibernate.search.backend.lucene_version", context.configuration().luceneVersion() )
				.property( "hibernate.search.configuration_property_checking.strategy", "ignore" )
				.property(
						StandalonePojoMapperSettings.MAPPING_CONFIGURER,
						BeanReference.ofInstance( (StandalonePojoMappingConfigurer) configurationContext -> {
							// disable any discovery by annotations -- that won't work as we do not implement annotations in the pojo model
							configurationContext.annotationMapping()
									.discoverAnnotatedTypesFromRootMappingAnnotations( false )
									.discoverAnnotationsFromReferencedTypes( false )
									.discoverJandexIndexesFromAddedTypes( false )
									.buildMissingDiscoveredJandexIndexes( false );

							configurationContext.bridges()
									.subTypesOf( HibernateSearchProcessorEnum.class )
									.valueBinder( HibernateSearchProcessorEnum.BINDER )
									.identifierBinder( HibernateSearchProcessorEnum.BINDER );

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
				).build().boot() ) {

			boolean ormMapperPresent = context.isOrmMapperPresent();

			for ( SearchIndexedEntity<?> entity : searchMapping.allIndexedEntities() ) {
				TypeElement typeElement = introspectorContext.typeElementsByName( entity.name() );
				String packageName = context.elementUtils().getPackageOf( typeElement ).getQualifiedName().toString();

				MetamodelClassWriter builder =
						new MetamodelClassWriter( ormMapperPresent, context.configuration(), MetamodelNamesFormatter.DEFAULT,
								packageName,
								entity.name() );

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
	}

	public static void processTypeAndProperties(TypeElement typeElement, TypeMappingStep typeMappingContext,
			ProcessorAnnotationProcessorContext ctx) {
		if ( !ctx.processedTypes().add( typeElement ) ) {
			return;
		}

		flattenedAnnotations( ctx.types(), typeElement )
				.forEach( annotationMirror -> ProcessorTypeMappingAnnotationProcessor.processor( annotationMirror )
						.ifPresent( p -> p.process(
								typeMappingContext,
								annotationMirror,
								typeElement,
								ctx
						) ) );

		AtomicReference<PropertyMappingStep> documentId = new AtomicReference<>();
		AtomicReference<PropertyMappingStep> ormId = new AtomicReference<>();
		propertyElements( ctx.elements(), typeElement )
				.forEach( element -> {
					PropertyMappingStep step = typeMappingContext.property( propertyName( element ) );

					try {
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
					}
					catch (Exception e) {
						ExceptionUtils.logError( ctx.messager(), e,
								"Unable to process Hibernate Search metamodel annotations: ", element );
					}
				} );
		PropertyMappingStep docIdStep = null;
		if ( documentId.get() != null ) {
			docIdStep = documentId.get();
		}
		if ( ormId.get() != null ) {
			docIdStep = ormId.get();
		}
		if ( docIdStep != null ) {
			// Users can have custom identifier binders/bridges, but we don't care much about their impl
			// and since for now we are ignoring these impls we still want users to be able to generate
			// a partial model; without an id that wouldn't be possible... hence:
			docIdStep.documentId().identifierBinder( ProcessorIdentifierBinder.INSTANCE );
		}
	}

	private ProcessorPojoModelsBootstrapIntrospector wrapIntrospector(PojoBootstrapIntrospector introspector) {
		return new ProcessorPojoModelsBootstrapIntrospector( introspectorContext, introspector );
	}

	@SuppressJQAssistant(reason = "Need to cast to an impl type to get access to not-yet exposed method")
	private static class ProcessorIdentifierBinder implements IdentifierBinder {

		static ProcessorIdentifierBinder INSTANCE = new ProcessorIdentifierBinder();

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void bind(IdentifierBindingContext<?> context) {
			if ( context instanceof DefaultIdentifierBindingContext ctx ) {
				ctx.applyBridge( Object.class, BeanHolder.of( ProcessorIdentifierBridge.INSTANCE ) );
			}
			else {
				context.bridge( Object.class, ProcessorIdentifierBridge.INSTANCE );
			}
		}

		private static class ProcessorIdentifierBridge implements IdentifierBridge<Object> {

			static ProcessorIdentifierBridge INSTANCE = new ProcessorIdentifierBridge();

			@Override
			public String toDocumentIdentifier(Object propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
				return "";
			}

			@Override
			public Object fromDocumentIdentifier(String documentIdentifier,
					IdentifierBridgeFromDocumentIdentifierContext context) {
				return null;
			}
		}
	}

}
