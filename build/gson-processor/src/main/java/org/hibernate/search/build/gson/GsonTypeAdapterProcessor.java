/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.gson;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("org.hibernate.search.backend.elasticsearch.gson.impl.GsonSerializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class GsonTypeAdapterProcessor extends AbstractProcessor {

	private static final String GSON_SERIALIZABLE_FQN =
			"org.hibernate.search.backend.elasticsearch.gson.impl.GsonSerializable";

	private final TypeAdapterFactoryWriter writer =
			new TypeAdapterFactoryWriter( GsonTypeAdapterProcessor.class.getName() );

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if ( roundEnv.processingOver() ) {
			return false;
		}

		TypeElement annotation = processingEnv.getElementUtils().getTypeElement( GSON_SERIALIZABLE_FQN );
		if ( annotation == null ) {
			return false;
		}

		for ( Element element : roundEnv.getElementsAnnotatedWith( annotation ) ) {
			processClass( (TypeElement) element );
		}

		return false;
	}

	private void processClass(TypeElement classElement) {
		ClassModel model = ClassModel.build( classElement, processingEnv );
		if ( model == null ) {
			return;
		}

		try {
			writer.write( model, classElement, processingEnv.getFiler() );
		}
		catch (IOException e) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR,
					"Failed to generate adapter: " + e.getMessage(), classElement );
		}
	}
}
