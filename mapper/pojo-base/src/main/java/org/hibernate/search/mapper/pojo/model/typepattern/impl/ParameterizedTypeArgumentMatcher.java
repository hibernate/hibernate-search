/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.lang.reflect.TypeVariable;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class ParameterizedTypeArgumentMatcher implements ExtractingTypePatternMatcher {
	private final Class<?> matchedRawType;
	private final int resultTypeParameterIndex;

	ParameterizedTypeArgumentMatcher(Class<?> matchedRawType, int resultTypeParameterIndex) {
		this.matchedRawType = matchedRawType;
		this.resultTypeParameterIndex = resultTypeParameterIndex;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( matchedRawType.getName() ).append( "<" );
		TypeVariable<? extends Class<?>>[] typeParameters = matchedRawType.getTypeParameters();
		for ( int i = 0; i < typeParameters.length; ++i ) {
			if ( i > 0 ) {
				builder.append( ", " );
			}
			if ( i == resultTypeParameterIndex ) {
				builder.append( "T" );
			}
			else {
				builder.append( "?" );
			}
		}
		builder.append( ">" ).append( " => T" );
		return builder.toString();
	}


	@Override
	public Optional<? extends PojoTypeModel<?>> extract(PojoTypeModel<?> typeToInspect) {
		return typeToInspect.typeArgument( matchedRawType, resultTypeParameterIndex );
	}
}
