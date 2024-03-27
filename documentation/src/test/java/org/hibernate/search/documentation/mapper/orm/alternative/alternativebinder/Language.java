/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;

//tag::include[]
public enum Language { // <1>

	ENGLISH( "en" ),
	FRENCH( "fr" ),
	GERMAN( "de" );

	public final String code;

	Language(String code) {
		this.code = code;
	}
}
//end::include[]
