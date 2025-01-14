/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.writer.impl;

import java.util.Locale;

public record ValueFieldReferenceDetails(TypedFieldReferenceDetails typedField) {

	public String formatted() {
		// I - dsl type (input) ValueModel.MAPPING
		// O - projection type (output) ValueModel.MAPPING
		// T - index type, i.e. no converters applied i.e. ValueModel.INDEX
		// R - raw type. i.e. ValueModel.RAW
		String name = name();
		typedField.asType( "I", "O" );
		String typeIndex = typedField.asType( "T", "T" );
		String typeRaw = typedField.asType( "R", "R" );
		String typeString = typedField.asType( "java.lang.String", "java.lang.String" );
		return String.format( Locale.ROOT, """
				public static class %s<SR, I, O, T, R> extends %s {

					private final %s mapping;
					private final %s raw;
					private final %s string;

					public %s(
						String absolutePath,
						Class<SR> scopeRootType,
						Class<I> inputType,
						Class<O> outputType,
						Class<T> indexType,
						Class<R> rawType
					) {
					%s
					this.mapping = %s
					this.raw = %s
					this.string = %s
					}

					public %s mapping() {
						return mapping;
					}

					public %s raw() {
						return raw;
					}

					public %s string() {
						return string;
					}

				}
				""",
				name,
				typedField.asType( "I", "O" ),
				typeIndex,
				typeRaw,
				typeString,
				name,
				typedField.constructorSuperCall( "org.hibernate.search.engine.search.common.ValueModel.MAPPING", "inputType",
						"outputType" ),
				typedField.constructorCall( "org.hibernate.search.engine.search.common.ValueModel.INDEX", "indexType",
						"indexType" ),
				typedField.constructorCall( "org.hibernate.search.engine.search.common.ValueModel.RAW", "rawType", "rawType" ),
				typedField.constructorCall( "org.hibernate.search.engine.search.common.ValueModel.STRING",
						"java.lang.String.class", "java.lang.String.class" ),
				typeIndex,
				typeRaw,
				typeString
		);
	}

	public String formattedWithTypedField() {
		return formatted() + "\n" + typedField.formatted();
	}

	private String name() {
		return "ValueFieldReference" + typedField.identifier();
	}

	public String asType(String scopeType, String inputType, String outputType, String indexType, String rawType) {
		return name() + "<" + scopeType + ", " + inputType + ", " + outputType + ", " + indexType + ", " + rawType + ">";
	}

	public String constructorCall(String name, String scopeType, String inputType, String outputType, String indexType,
			String rawType) {
		return "new " + name() + "<>(\"" + name + "\", " + scopeType + ".class, " + inputType + ".class, " + outputType
				+ ".class, " + indexType + ".class, " + rawType + ".class);";
	}
}
