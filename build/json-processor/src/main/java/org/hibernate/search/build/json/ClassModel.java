package org.hibernate.search.build.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

final class ClassModel {

	private static final String SERIALIZE_EXTRA_PROPERTIES_FQN =
			"org.hibernate.search.backend.elasticsearch.json.impl.SerializeExtraProperties";
	private static final String SERIALIZED_NAME_FQN = "org.hibernate.search.backend.elasticsearch.json.impl.JsonName";
	private static final String FIELD_CODEC_FQN = "org.hibernate.search.backend.elasticsearch.json.impl.JsonFieldCodec";

	final String packageName;
	final String qualifiedName;
	final String codecSimpleName;
	final String codecQualifiedName;
	final List<FieldModel> fields;
	final String extraPropsGetter;
	final String extraPropsSetter;

	private ClassModel(String packageName, String qualifiedName, String codecSimpleName,
			List<FieldModel> fields, String extraPropsGetter, String extraPropsSetter) {
		this.packageName = packageName;
		this.qualifiedName = qualifiedName;
		this.codecSimpleName = codecSimpleName;
		this.codecQualifiedName = packageName + "." + codecSimpleName;
		this.fields = fields;
		this.extraPropsGetter = extraPropsGetter;
		this.extraPropsSetter = extraPropsSetter;
	}

	boolean hasExtraProperties() {
		return extraPropsGetter != null;
	}

	static ClassModel build(TypeElement classElement, ProcessingEnvironment processingEnv) {
		if ( classElement.getModifiers().contains( Modifier.ABSTRACT ) ) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR,
					"@JsonSerializable cannot be used on abstract classes", classElement );
			return null;
		}

		String packageName = processingEnv.getElementUtils()
				.getPackageOf( classElement ).getQualifiedName().toString();
		String qualifiedName = classElement.getQualifiedName().toString();
		String codecSimpleName = "Generated" + simpleClassName( classElement ) + "JsonCodec";

		List<FieldModel> fields = new ArrayList<>();
		String extraPropsGetter = null;
		String extraPropsSetter = null;

		TypeElement current = classElement;
		while ( current != null && !isObject( current ) ) {
			for ( Element enclosed : current.getEnclosedElements() ) {
				if ( enclosed.getKind() != ElementKind.FIELD
						|| enclosed.getModifiers().contains( Modifier.STATIC ) ) {
					continue;
				}
				VariableElement field = (VariableElement) enclosed;
				if ( hasAnnotation( field, SERIALIZE_EXTRA_PROPERTIES_FQN ) ) {
					if ( extraPropsGetter == null ) {
						extraPropsGetter = findAccessor( classElement, field, AccessorKind.GETTER, processingEnv );
						extraPropsSetter = findAccessor( classElement, field, AccessorKind.SETTER, processingEnv );
					}
					continue;
				}
				FieldModel fieldModel = buildField( classElement, field, processingEnv );
				if ( fieldModel != null ) {
					fields.add( fieldModel );
				}
			}
			current = superclass( current, processingEnv );
		}

		// Sort for reproducible output regardless of compiler element ordering
		fields.sort( Comparator.comparing( f -> f.javaName ) );
		return new ClassModel( packageName, qualifiedName, codecSimpleName,
				fields, extraPropsGetter, extraPropsSetter );
	}

	private static FieldModel buildField(TypeElement rootClass, VariableElement field,
			ProcessingEnvironment processingEnv) {
		String javaName = field.getSimpleName().toString();
		String jsonName = javaName;
		List<String> alternateNames = Collections.emptyList();

		AnnotationMirror serializedNameAnnotation = getAnnotationMirror( field, SERIALIZED_NAME_FQN );
		if ( serializedNameAnnotation != null ) {
			for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : serializedNameAnnotation
					.getElementValues().entrySet() ) {
				String key = entry.getKey().getSimpleName().toString();
				if ( "value".equals( key ) ) {
					jsonName = (String) entry.getValue().getValue();
				}
				else if ( "alternate".equals( key ) ) {
					@SuppressWarnings("unchecked")
					List<? extends AnnotationValue> altValues =
							(List<? extends AnnotationValue>) entry.getValue().getValue();
					alternateNames = new ArrayList<>();
					for ( AnnotationValue av : altValues ) {
						alternateNames.add( (String) av.getValue() );
					}
				}
			}
		}

		String getter = findAccessor( rootClass, field, AccessorKind.GETTER, processingEnv );
		String setter = findAccessor( rootClass, field, AccessorKind.SETTER, processingEnv );
		if ( getter == null || setter == null ) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE,
					"Field " + javaName + " in " + rootClass.getQualifiedName()
							+ " is missing a public getter or setter, skipping",
					field );
			return null;
		}

		String customCodecClass = null;
		AnnotationMirror fieldCodecAnnotation = getAnnotationMirror( field, FIELD_CODEC_FQN );
		if ( fieldCodecAnnotation != null ) {
			for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : fieldCodecAnnotation
					.getElementValues().entrySet() ) {
				if ( "value".equals( entry.getKey().getSimpleName().toString() ) ) {
					TypeMirror codecType = (TypeMirror) entry.getValue().getValue();
					customCodecClass = codecType.toString();
				}
			}
		}

		return new FieldModel( javaName, jsonName, alternateNames, field.asType(), getter, setter, customCodecClass );
	}

	private enum AccessorKind {
		GETTER, SETTER
	}

	private static String findAccessor(TypeElement rootClass, VariableElement field,
			AccessorKind kind, ProcessingEnvironment processingEnv) {
		String fieldName = field.getSimpleName().toString();
		String capitalizedName = Character.toUpperCase( fieldName.charAt( 0 ) ) + fieldName.substring( 1 );

		List<String> candidates = new ArrayList<>();
		int expectedParams;

		if ( kind == AccessorKind.GETTER ) {
			candidates.add( "get" + capitalizedName );
			candidates.add( "is" + capitalizedName );
			expectedParams = 0;
		}
		else {
			candidates.add( "set" + capitalizedName );
			expectedParams = 1;
		}

		// Handle "isXxx" fields where the accessor strips the "is" prefix
		if ( fieldName.length() > 2
				&& fieldName.startsWith( "is" )
				&& Character.isUpperCase( fieldName.charAt( 2 ) ) ) {
			String stripped = fieldName.substring( 2 );
			candidates.add( ( kind == AccessorKind.GETTER ? "get" : "set" ) + stripped );
		}

		TypeElement current = rootClass;
		while ( current != null && !isObject( current ) ) {
			for ( Element enclosed : current.getEnclosedElements() ) {
				if ( enclosed.getKind() != ElementKind.METHOD
						|| !enclosed.getModifiers().contains( Modifier.PUBLIC ) ) {
					continue;
				}
				ExecutableElement method = (ExecutableElement) enclosed;
				if ( method.getParameters().size() == expectedParams
						&& candidates.contains( method.getSimpleName().toString() ) ) {
					return method.getSimpleName().toString();
				}
			}
			current = superclass( current, processingEnv );
		}
		return null;
	}

	private static String simpleClassName(TypeElement element) {
		Element enclosing = element.getEnclosingElement();
		if ( enclosing.getKind() == ElementKind.CLASS ) {
			return simpleClassName( (TypeElement) enclosing ) + "_" + element.getSimpleName();
		}
		return element.getSimpleName().toString();
	}

	private static boolean isObject(TypeElement element) {
		return element.getQualifiedName().toString().equals( "java.lang.Object" );
	}

	private static TypeElement superclass(TypeElement element, ProcessingEnvironment processingEnv) {
		TypeMirror superclass = element.getSuperclass();
		if ( superclass.getKind() == TypeKind.NONE ) {
			return null;
		}
		return (TypeElement) processingEnv.getTypeUtils().asElement( superclass );
	}

	private static boolean hasAnnotation(Element element, String annotationFqn) {
		return getAnnotationMirror( element, annotationFqn ) != null;
	}

	private static AnnotationMirror getAnnotationMirror(Element element, String annotationFqn) {
		for ( AnnotationMirror am : element.getAnnotationMirrors() ) {
			if ( am.getAnnotationType().toString().equals( annotationFqn ) ) {
				return am;
			}
		}
		return null;
	}
}
