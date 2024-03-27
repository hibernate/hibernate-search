/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class BeanReferences {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private BeanReferences() {
	}

	public static <T> BeanReference<T> parse(Class<T> expectedType, String value) {
		Contracts.assertNotNull( expectedType, "expectedType" );
		Contracts.assertNotNullNorEmpty( value, "value" );

		BeanRetrieval retrieval;
		int colonIndex = value.indexOf( ':' );
		String name;
		if ( colonIndex < 0 ) {
			retrieval = BeanRetrieval.ANY;
			name = value;
		}
		else {
			String retrievalAsString = value.substring( 0, colonIndex );
			try {
				retrieval = BeanRetrieval.valueOf( retrievalAsString.toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException e) {
				throw log.invalidBeanRetrieval( value, retrievalAsString + ':',
						Arrays.stream( BeanRetrieval.values() )
								.map( v -> v.name().toLowerCase( Locale.ROOT ) + ':' )
								.collect( Collectors.toList() ),
						e );
			}
			name = value.substring( colonIndex + 1 );
		}
		return BeanReference.of( expectedType, name, retrieval );
	}
}
