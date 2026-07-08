/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.gson;

import java.io.PrintWriter;
import java.util.List;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

final class FieldModel {

	final String javaName;
	final String jsonName;
	final List<String> alternateNames;
	final TypeMirror typeMirror;
	final String getter;
	final String setter;
	final String customAdapterClass;

	private String typeTokenConstantName;

	FieldModel(String javaName, String jsonName, List<String> alternateNames,
			TypeMirror typeMirror, String getter, String setter, String customAdapterClass) {
		this.javaName = javaName;
		this.jsonName = jsonName;
		this.alternateNames = alternateNames;
		this.typeMirror = typeMirror;
		this.getter = getter;
		this.setter = setter;
		this.customAdapterClass = customAdapterClass;
	}

	boolean hasCustomAdapter() {
		return customAdapterClass != null;
	}

	boolean needsTypeToken() {
		return !hasCustomAdapter()
				&& typeMirror instanceof DeclaredType
				&& !( (DeclaredType) typeMirror ).getTypeArguments().isEmpty();
	}

	void assignTypeTokenConstant(String name) {
		this.typeTokenConstantName = name;
	}

	void writeTypeTokenConstant(PrintWriter w) {
		w.println( "\tprivate static final TypeToken<" + typeMirror + "> " + typeTokenConstantName + " =" );
		w.println( "\t\t\tnew TypeToken<" + typeMirror + ">() {};" );
	}

	void writeAdapterField(PrintWriter w) {
		w.println( "\t\tprivate final TypeAdapter<" + typeMirror + "> " + adapterFieldName() + ";" );
	}

	void writeAdapterInit(PrintWriter w) {
		if ( hasCustomAdapter() ) {
			w.println( "\t\t\tthis." + adapterFieldName() + " = new " + customAdapterClass + "();" );
		}
		else if ( typeTokenConstantName != null ) {
			w.println( "\t\t\tthis." + adapterFieldName() + " = gson.getAdapter( " + typeTokenConstantName + " );" );
		}
		else {
			w.println( "\t\t\tthis." + adapterFieldName() + " = gson.getAdapter( " + typeMirror + ".class );" );
		}
	}

	void writeReadCase(PrintWriter w) {
		w.println( "\t\t\t\t\tcase \"" + jsonName + "\":" );
		for ( String alt : alternateNames ) {
			w.println( "\t\t\t\t\tcase \"" + alt + "\":" );
		}
		w.println( "\t\t\t\t\t\tinstance." + setter + "( " + adapterFieldName() + ".read( in ) );" );
		w.println( "\t\t\t\t\t\tbreak;" );
	}

	void writeWriteStatement(PrintWriter w) {
		w.println( "\t\t\tout.name( \"" + jsonName + "\" );" );
		w.println( "\t\t\t" + adapterFieldName() + ".write( out, instance." + getter + "() );" );
	}

	private String adapterFieldName() {
		return javaName + "Adapter";
	}
}
