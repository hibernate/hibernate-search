/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.writer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.engine.search.common.ValueModel;

record TypedFieldReferenceDetails(  String name, String identifier, Collection<String> typeVariables,
									List<ClassProperty> properties, Collection<String> implementedInterfaces) {
	public static TypedFieldReferenceDetails of(Collection<TraitReferenceDetails> traits) {
		StringBuilder name = new StringBuilder();
		Set<String> typeVariables = new LinkedHashSet<>();
		List<String> implementedInterfaces = new ArrayList<>();
		Set<ClassProperty> properties = new LinkedHashSet<>();
		Set<ClassProperty> extraProperties = new TreeSet<>();

		properties.add( new ClassProperty( "java.lang.String", "absolutePath" ) );
		properties.add( new ClassProperty( "java.lang.Class<SR>", "scopeRootType" ) );

		typeVariables.add( "SR" );
		boolean requiresValueModel = false;
		boolean requiresInputType = false;
		boolean requiresOutputType = false;
		for ( TraitReferenceDetails trait : traits ) {
			name.append( trait.implementationLabel() );
			implementedInterfaces.add( trait.asString( "I", "O" ) );
			requiresValueModel = requiresValueModel || trait.traitKind().requiresValueModel();
			requiresInputType = requiresInputType || trait.traitKind().requiresInputType();
			requiresOutputType = requiresOutputType || trait.traitKind().requiresOutputType();
			trait.formatExtraProperty( "I", "O" ).ifPresent( extraProperties::add );
		}
		if ( requiresValueModel ) {
			properties.add( new ClassProperty( ValueModel.class.getName(), "valueModel" ) );
		}
		if ( requiresInputType ) {
			typeVariables.add( "I" );
		}
		if ( requiresOutputType ) {
			typeVariables.add( "O" );
		}
		List<ClassProperty> allProperties = new ArrayList<>( properties );
		allProperties.addAll( extraProperties );

		String formattedName = name.toString();
		return new TypedFieldReferenceDetails(
				"TypedFieldReference" + formattedName,
				formattedName, typeVariables, allProperties,
				implementedInterfaces );
	}

	public String constructorCall(String valueModel, String input, String output) {
		StringBuilder result = new StringBuilder( "new " );
		result.append( name() );
		result.append( "<>" );
		constructorCall( result, valueModel, input, output );
		result.append( ";" );

		return result.toString();
	}

	public String constructorSuperCall(String valueModel, String input, String output) {
		StringBuilder result = new StringBuilder( "super" );
		constructorCall( result, valueModel, input, output );
		result.append( ";" );

		return result.toString();
	}

	public String asType(String input, String output) {
		StringBuilder result = new StringBuilder( name() );
		result.append( "<SR" );
		if ( typeVariables.contains( "I" ) ) {
			result.append( ", " ).append( input );
		}
		if ( typeVariables.contains( "O" ) ) {
			result.append( ", " ).append( output );
		}
		result.append( ">" );
		return result.toString();
	}

	private void constructorCall(StringBuilder result, String valueModel, String input, String output) {
		result.append( "(absolutePath, scopeRootType" );
		if ( properties().size() > 2 ) {
			result.append( ", " )
					.append( valueModel );
		}
		for ( int i = 3; i < properties().size(); i++ ) {
			ClassProperty prop = properties().get( i );
			if ( prop.type().contains( "<I>" ) ) {
				result.append( ", " )
						.append( input );
			}
			if ( prop.type().contains( "<O>" ) ) {
				result.append( ", " )
						.append( output );
			}
		}
		result.append( ")" );
	}

	public String formatted() {
		return String.format( Locale.ROOT, """
				public static class %s<%s> implements %s {

					private final %s;

					public %s(
						%s) {
						%s;
					}

					%s
				}
				""",
				name,
				String.join( ", ", typeVariables ),
				String.join( ",\n\t", implementedInterfaces ),
				String.join( ";\n\tprivate final ", properties.stream().map( ClassProperty::asParameter ).toList() ),
				name,
				String.join( ",\n\t\t", properties.stream().map( ClassProperty::asParameter ).toList() ),
				String.join( ";\n\t\t", properties.stream().map( ClassProperty::asSetInConstructor ).toList() ),
				String.join( "\n\t", properties.stream().map( ClassProperty::asGetter ).toList() )
		);
	}
}
