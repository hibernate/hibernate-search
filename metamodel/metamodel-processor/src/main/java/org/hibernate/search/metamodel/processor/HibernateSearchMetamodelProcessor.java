/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
@SupportedOptions({
		HibernateSearchMetamodelProcessorSettings.ADD_GENERATED_ANNOTATION,
		HibernateSearchMetamodelProcessorSettings.BACKEND_VERSION })
@org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant(
		reason = "JQAssistant has issue with detecting that getSupportedSourceVersion is an overridden method.")
public class HibernateSearchMetamodelProcessor extends AbstractProcessor {

	private HibernateSearchMetamodelProcessorContext context;
	private List<MetamodelAnnotationProcessor> processors;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init( processingEnv );
		context =
				new HibernateSearchMetamodelProcessorContext( processingEnv.getElementUtils(), processingEnv.getTypeUtils(),
						processingEnv.getMessager(), processingEnv.getFiler(),
						new HibernateSearchMetamodelProcessorSettings.Configuration( processingEnv.getOptions() ) );
		processors = List.of( new IndexedEntityMetamodelAnnotationProcessor( context ) );
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for ( MetamodelAnnotationProcessor processor : processors ) {
			try {
				processor.process( roundEnv );
			}
			catch (Throwable e) {
				try ( var sw = new StringWriter(); var pw = new PrintWriter( sw ) ) {
					e.printStackTrace( pw );
					context.messager().printMessage( Diagnostic.Kind.ERROR, sw.toString() );
				}
				catch (IOException ex) {
					throw new RuntimeException( ex );
				}
			}
		}
		return false;
	}
}
