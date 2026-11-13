package org.hibernate.search.build.json;

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

@SupportedAnnotationTypes("org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class JsonCodecProcessor extends AbstractProcessor {

	private static final String JSON_SERIALIZABLE_FQN =
			"org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable";

	private final JsonCodecWriter writer =
			new JsonCodecWriter( JsonCodecProcessor.class.getName() );

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if ( roundEnv.processingOver() ) {
			return false;
		}

		TypeElement annotation = processingEnv.getElementUtils().getTypeElement( JSON_SERIALIZABLE_FQN );
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
		catch (IOException | IllegalArgumentException e) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR,
					"Failed to generate JSON codec: " + e.getMessage(), classElement );
		}
	}
}
