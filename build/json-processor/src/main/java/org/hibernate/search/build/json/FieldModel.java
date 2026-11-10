package org.hibernate.search.build.json;

import java.util.List;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

final class FieldModel {
	final String javaName, jsonName, getter, setter, customCodecClass;
	final List<String> alternateNames;
	final TypeMirror typeMirror;

	FieldModel(String javaName, String jsonName, List<String> alternateNames, TypeMirror typeMirror, String getter,
			String setter, String customCodecClass) {
		this.javaName = javaName;
		this.jsonName = jsonName;
		this.alternateNames = alternateNames;
		this.typeMirror = typeMirror;
		this.getter = getter;
		this.setter = setter;
		this.customCodecClass = customCodecClass;
	}

	String codecExpression() {
		return customCodecClass == null ? expression( typeMirror ) : "new " + customCodecClass + "()";
	}

	private static String expression(TypeMirror type) {
		String name = type.toString();
		String scalar = switch ( name ) {
			case "java.lang.String" -> "STRING";
			case "java.lang.Boolean" -> "BOOLEAN";
			case "java.lang.Integer" -> "INTEGER";
			case "java.lang.Long" -> "LONG";
			case "java.lang.Double" -> "DOUBLE";
			case "java.lang.Float" -> "FLOAT";
			case "tools.jackson.databind.JsonNode" -> "ELEMENT";
			default -> null;
		};
		if ( scalar != null ) {
			return "JsonCodecs." + scalar;
		}
		if ( type instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty() ) {
			String raw = declared.asElement().toString();
			var args = declared.getTypeArguments();
			if ( raw.equals( "java.util.List" ) && args.size() == 1 ) {
				return "JsonCodecs.list(" + expression( args.get( 0 ) ) + ")";
			}
			if ( raw.equals( "java.util.Map" ) && args.size() == 2 && args.get( 0 ).toString().equals( "java.lang.String" ) ) {
				return "JsonCodecs.map(" + expression( args.get( 1 ) ) + ")";
			}
			throw new IllegalArgumentException( "Unsupported JSON codec field type: " + type );
		}
		if ( !( type instanceof DeclaredType ) ) {
			throw new IllegalArgumentException( "Unsupported JSON codec field type: " + type );
		}
		return "registry.reference(" + type + ".class)";
	}
}
