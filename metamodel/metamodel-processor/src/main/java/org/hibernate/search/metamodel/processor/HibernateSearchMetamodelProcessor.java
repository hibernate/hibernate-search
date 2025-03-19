/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.hibernate.search.metamodel.processor.impl.HibernateSearchMetamodelProcessorContext;
import org.hibernate.search.metamodel.processor.impl.IndexedEntityMetamodelAnnotationProcessor;
import org.hibernate.search.metamodel.processor.impl.MetamodelAnnotationProcessor;

// We inspect all annotations and then decide if we can process them,
// this way we can also work with user-defined ones (at some point):
@SupportedAnnotationTypes("*")
// Currently this is more of a placeholder for future config options:
@SupportedOptions({ HibernateSearchMetamodelProcessorSettings.ADD_GENERATED_ANNOTATION })
@org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant(
		reason = "JQAssistant has issue with detecting that getSupportedSourceVersion is an overridden method.")
public class HibernateSearchMetamodelProcessor extends AbstractProcessor {

	private HibernateSearchMetamodelProcessorContext context;
	private HibernateSearchMetamodelProcessorSettings.Configuration configuration;
	private List<MetamodelAnnotationProcessor> processors;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init( processingEnv );
		context = new HibernateSearchMetamodelProcessorContext( processingEnv.getElementUtils(), processingEnv.getTypeUtils(),
				processingEnv.getMessager(), processingEnv.getFiler() );
		configuration = new HibernateSearchMetamodelProcessorSettings.Configuration( processingEnv.getOptions() );
		processors = List.of( new IndexedEntityMetamodelAnnotationProcessor( context ) );
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		context.messager().printMessage( Diagnostic.Kind.NOTE, "Hibernate Search Metamodel Processor started" );
		for ( MetamodelAnnotationProcessor processor : processors ) {
			processor.process( roundEnv );
		}
		if ( roundEnv.processingOver() ) {
			// create metamodel classes
		}
		return false;
	}
}
