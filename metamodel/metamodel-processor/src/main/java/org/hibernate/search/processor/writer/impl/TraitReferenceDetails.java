/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.writer.impl;

import java.util.Optional;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
record TraitReferenceDetails(   Class<?> referenceClass, String implementationLabel, TraitKind traitKind,
								String extraPropertyName)
		implements Comparable<TraitReferenceDetails> {

	public TraitReferenceDetails(Class<?> referenceClass, String implementationLabel, TraitKind traitKind) {
		this( referenceClass, implementationLabel, traitKind, null );
	}

	public String asString(String input, String output) {
		StringBuilder result = new StringBuilder( referenceClass().getName() );
		result.append( "<SR" );
		if ( traitKind().requiresInputType() ) {
			result.append( ", " )
					.append( input );
		}
		if ( traitKind().requiresOutputType() ) {
			result.append( ", " )
					.append( output );
		}
		result.append( ">" );
		return result.toString();
	}

	public Optional<ClassProperty> formatExtraProperty(String input, String output) {
		if ( extraPropertyName == null ) {
			return Optional.empty();
		}
		StringBuilder result = new StringBuilder( "Class<" );
		if ( traitKind().requiresInputType() ) {
			result.append( input );
		}
		if ( traitKind().requiresOutputType() ) {
			result.append( output );
		}
		result.append( ">" );
		return Optional.of( new ClassProperty( result.toString(), extraPropertyName() ) );
	}

	@Override
	public int compareTo(TraitReferenceDetails o) {
		return implementationLabel().compareTo( o.implementationLabel() );
	}
}
