package org.hibernate.search.build.json;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;

final class JsonCodecWriter {
	private final String processorClassName;

	JsonCodecWriter(String processorClassName) {
		this.processorClassName = processorClassName;
	}

	void write(ClassModel m, TypeElement element, Filer filer) throws IOException {
		SourceImports imports = new SourceImports( m.packageName, element );
		imports.add( m.codecQualifiedName );
		for ( String type : new String[] {
				"tools.jackson.databind.JsonNode",
				"tools.jackson.databind.node.ObjectNode",
				"tools.jackson.databind.node.NullNode",
				"org.hibernate.search.util.json.internal.spi.JsonNodes",
				"org.hibernate.search.util.json.internal.spi.JsonCodec",
				"org.hibernate.search.util.json.internal.spi.JsonCodecs",
				"org.hibernate.search.util.json.internal.spi.JsonCodecRegistry",
				"javax.annotation.processing.Generated",
				"java.lang.Override" } ) {
			imports.add( type );
		}
		if ( m.hasExtraProperties() ) {
			imports.add( "java.util.LinkedHashMap" );
		}
		imports.add( m.qualifiedName );
		// Validate every field and collect its referenced types before opening the generated source.
		for ( FieldModel f : m.fields ) {
			f.codecExpression();
			imports.add( f.typeMirror );
			if ( f.customCodecClass != null ) {
				imports.add( f.customCodecClass );
			}
		}
		try ( PrintWriter w = new PrintWriter( filer.createSourceFile( m.codecQualifiedName, element ).openWriter() ) ) {
			line( w, 0, "package " + m.packageName + ";" );
			w.println();
			String previousGroup = null;
			for ( String name : imports.declarations() ) {
				String group = name.substring( 0, name.indexOf( '.' ) );
				if ( previousGroup != null && !previousGroup.equals( group ) ) {
					w.println();
				}
				line( w, 0, "import " + name + ";" );
				previousGroup = group;
			}
			w.println();
			line( w, 0, "@Generated(\"" + processorClassName + "\")" );
			line( w, 0, "public final class " + m.codecSimpleName + " implements JsonCodec<"
					+ imports.shorten( m.qualifiedName ) + "> {" );
			for ( FieldModel f : m.fields ) {
				line( w, 1, "private final JsonCodec<" + imports.shorten( f.typeMirror.toString() ) + "> " + f.javaName
						+ "Codec;" );
			}
			w.println();
			line( w, 1, "public " + m.codecSimpleName + "(JsonCodecRegistry registry) {" );
			for ( FieldModel f : m.fields ) {
				line( w, 2, "this." + f.javaName + "Codec = " + imports.shorten( f.codecExpression() ) + ";" );
			}
			line( w, 1, "}" );
			w.println();
			line( w, 1, "@Override" );
			line( w, 1, "public " + imports.shorten( m.qualifiedName ) + " decode(JsonNode value) {" );
			line( w, 2, "if ( JsonCodecs.isNull( value ) ) {" );
			line( w, 3, "return null;" );
			line( w, 2, "}" );
			line( w, 2, imports.shorten( m.qualifiedName ) + " instance = new " + imports.shorten( m.qualifiedName ) + "();" );
			line( w, 2, "for ( var entry : ((ObjectNode) value).properties() ) {" );
			line( w, 3, "switch ( entry.getKey() ) {" );
			for ( FieldModel f : m.fields ) {
				line( w, 4, "case \"" + f.jsonName + "\":" );
				for ( String alt : f.alternateNames ) {
					line( w, 4, "case \"" + alt + "\":" );
				}
				line( w, 5, "instance." + f.setter + "( " + f.javaName + "Codec.decode( entry.getValue() ) );" );
				line( w, 5, "break;" );
			}
			line( w, 4, "default:" );
			if ( m.hasExtraProperties() ) {
				line( w, 5, "if ( instance." + m.extraPropsGetter + "() == null ) {" );
				line( w, 6, "instance." + m.extraPropsSetter + "( new LinkedHashMap<>() );" );
				line( w, 5, "}" );
				line( w, 5, "instance." + m.extraPropsGetter + "().put( entry.getKey(), entry.getValue() );" );
			}
			line( w, 5, "break;" );
			line( w, 3, "}" );
			line( w, 2, "}" );
			line( w, 2, "return instance;" );
			line( w, 1, "}" );
			w.println();
			line( w, 1, "@Override" );
			line( w, 1,
					"public JsonNode encode(" + imports.shorten( m.qualifiedName ) + " instance, boolean serializeNulls) {" );
			line( w, 2, "if ( instance == null ) {" );
			line( w, 3, "return NullNode.instance;" );
			line( w, 2, "}" );
			line( w, 2, "ObjectNode result = JsonNodes.objectNode();" );
			for ( FieldModel f : m.fields ) {
				line( w, 2,
						"JsonCodecs.put( result, \"" + f.jsonName + "\", " + f.javaName + "Codec.encode( instance." + f.getter
								+ "(), serializeNulls ), serializeNulls );" );
			}
			if ( m.hasExtraProperties() ) {
				line( w, 2, "if ( instance." + m.extraPropsGetter + "() != null ) {" );
				line( w, 3, "for ( var entry : instance." + m.extraPropsGetter + "().entrySet() ) {" );
				line( w, 4,
						"JsonCodecs.put( result, entry.getKey(), JsonCodecs.copy( entry.getValue(), serializeNulls ), serializeNulls );" );
				line( w, 3, "}" );
				line( w, 2, "}" );
			}
			line( w, 2, "return result;" );
			line( w, 1, "}" );
			line( w, 0, "}" );
		}
	}

	private static void line(PrintWriter writer, int indentation, String text) {
		writer.print( "\t".repeat( indentation ) );
		writer.println( text );
	}
}
