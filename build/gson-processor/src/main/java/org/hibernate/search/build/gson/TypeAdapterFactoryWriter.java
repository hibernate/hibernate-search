/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.gson;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

final class TypeAdapterFactoryWriter {

	private final String processorClassName;

	TypeAdapterFactoryWriter(String processorClassName) {
		this.processorClassName = processorClassName;
	}

	void write(ClassModel model, TypeElement originatingElement, Filer filer) throws IOException {
		JavaFileObject sourceFile = filer.createSourceFile( model.factoryQualifiedName, originatingElement );
		try ( PrintWriter w = new PrintWriter( sourceFile.openWriter() ) ) {
			writeHeader( w, model );
			writeTypeTokenConstants( w, model );
			writeCreateMethod( w, model );
			writeAdapterClass( w, model );
			w.println( "}" );
		}
	}

	private void writeHeader(PrintWriter w, ClassModel model) {
		w.println( "/*" );
		w.println( " * SPDX-License-Identifier: Apache-2.0" );
		w.println( " * Copyright Red Hat Inc. and Hibernate Authors" );
		w.println( " */" );
		w.println( "package " + model.packageName + ";" );
		w.println();
		w.println( "import java.io.IOException;" );
		if ( model.hasExtraProperties() ) {
			w.println( "import java.util.LinkedHashMap;" );
			w.println( "import java.util.Map;" );
		}
		w.println();
		w.println( "import com.google.gson.Gson;" );
		w.println( "import com.google.gson.JsonElement;" );
		w.println( "import com.google.gson.TypeAdapter;" );
		w.println( "import com.google.gson.TypeAdapterFactory;" );
		w.println( "import com.google.gson.reflect.TypeToken;" );
		w.println( "import com.google.gson.stream.JsonReader;" );
		w.println( "import com.google.gson.stream.JsonToken;" );
		w.println( "import com.google.gson.stream.JsonWriter;" );
		w.println();
		w.println( "@javax.annotation.processing.Generated(\"" + processorClassName + "\")" );
		w.println( "public class " + model.factorySimpleName + " implements TypeAdapterFactory {" );
		w.println();
	}

	private void writeTypeTokenConstants(PrintWriter w, ClassModel model) {
		boolean any = false;
		for ( FieldModel field : model.fields ) {
			if ( field.needsTypeToken() ) {
				field.writeTypeTokenConstant( w );
				any = true;
			}
		}
		if ( any ) {
			w.println();
		}
	}

	private void writeCreateMethod(PrintWriter w, ClassModel model) {
		w.println( "\t@Override" );
		w.println( "\t@SuppressWarnings(\"unchecked\")" );
		w.println( "\tpublic <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {" );
		w.println( "\t\tif ( !TypeToken.get( " + model.qualifiedName + ".class ).equals( type ) ) {" );
		w.println( "\t\t\treturn null;" );
		w.println( "\t\t}" );
		w.println( "\t\treturn (TypeAdapter<T>) new Adapter( gson );" );
		w.println( "\t}" );
		w.println();
	}

	private void writeAdapterClass(PrintWriter w, ClassModel model) {
		w.println( "\tprivate static class Adapter extends TypeAdapter<" + model.qualifiedName + "> {" );
		w.println();
		writeAdapterFields( w, model );
		writeAdapterConstructor( w, model );
		writeReadMethod( w, model );
		writeWriteMethod( w, model );
		w.println( "\t}" );
	}

	private void writeAdapterFields(PrintWriter w, ClassModel model) {
		for ( FieldModel field : model.fields ) {
			field.writeAdapterField( w );
		}
		if ( model.hasExtraProperties() ) {
			w.println( "\t\tprivate final TypeAdapter<JsonElement> jsonElementAdapter;" );
		}
		w.println();
	}

	private void writeAdapterConstructor(PrintWriter w, ClassModel model) {
		w.println( "\t\tAdapter(Gson gson) {" );
		for ( FieldModel field : model.fields ) {
			field.writeAdapterInit( w );
		}
		if ( model.hasExtraProperties() ) {
			w.println( "\t\t\tthis.jsonElementAdapter = gson.getAdapter( JsonElement.class );" );
		}
		w.println( "\t\t}" );
		w.println();
	}

	private void writeReadMethod(PrintWriter w, ClassModel model) {
		w.println( "\t\t@Override" );
		w.println( "\t\tpublic " + model.qualifiedName + " read(JsonReader in) throws IOException {" );
		w.println( "\t\t\tif ( in.peek() == JsonToken.NULL ) {" );
		w.println( "\t\t\t\tin.nextNull();" );
		w.println( "\t\t\t\treturn null;" );
		w.println( "\t\t\t}" );
		w.println();
		w.println( "\t\t\t" + model.qualifiedName + " instance = new " + model.qualifiedName + "();" );
		w.println( "\t\t\tin.beginObject();" );
		w.println( "\t\t\twhile ( in.hasNext() ) {" );
		w.println( "\t\t\t\tString name = in.nextName();" );
		w.println( "\t\t\t\tswitch ( name ) {" );

		for ( FieldModel field : model.fields ) {
			field.writeReadCase( w );
		}

		w.println( "\t\t\t\t\tdefault:" );
		if ( model.hasExtraProperties() ) {
			w.println( "\t\t\t\t\t\tJsonElement value = jsonElementAdapter.read( in );" );
			w.println( "\t\t\t\t\t\tif ( instance." + model.extraPropsGetter + "() == null ) {" );
			w.println( "\t\t\t\t\t\t\tinstance." + model.extraPropsSetter + "( new LinkedHashMap<>() );" );
			w.println( "\t\t\t\t\t\t}" );
			w.println( "\t\t\t\t\t\tinstance." + model.extraPropsGetter + "().put( name, value );" );
		}
		else {
			w.println( "\t\t\t\t\t\tin.skipValue();" );
		}
		w.println( "\t\t\t\t\t\tbreak;" );
		w.println( "\t\t\t\t}" );
		w.println( "\t\t\t}" );
		w.println( "\t\t\tin.endObject();" );
		w.println();
		w.println( "\t\t\treturn instance;" );
		w.println( "\t\t}" );
		w.println();
	}

	private void writeWriteMethod(PrintWriter w, ClassModel model) {
		w.println( "\t\t@Override" );
		w.println( "\t\tpublic void write(JsonWriter out, " + model.qualifiedName + " instance) throws IOException {" );
		w.println( "\t\t\tif ( instance == null ) {" );
		w.println( "\t\t\t\tout.nullValue();" );
		w.println( "\t\t\t\treturn;" );
		w.println( "\t\t\t}" );
		w.println( "\t\t\tout.beginObject();" );

		for ( FieldModel field : model.fields ) {
			field.writeWriteStatement( w );
		}

		if ( model.hasExtraProperties() ) {
			w.println( "\t\t\tjava.util.Map<String, JsonElement> extraProperties = instance."
					+ model.extraPropsGetter + "();" );
			w.println( "\t\t\tif ( extraProperties != null ) {" );
			w.println( "\t\t\t\tfor ( java.util.Map.Entry<String, JsonElement> entry : extraProperties.entrySet() ) {" );
			w.println( "\t\t\t\t\tout.name( entry.getKey() );" );
			w.println( "\t\t\t\t\tjsonElementAdapter.write( out, entry.getValue() );" );
			w.println( "\t\t\t\t}" );
			w.println( "\t\t\t}" );
		}

		w.println( "\t\t\tout.endObject();" );
		w.println( "\t\t}" );
	}
}
