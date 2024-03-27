/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.alternative;

public enum Language {

	ENGLISH( "en" ),
	FRENCH( "fr" ),
	GERMAN( "de" );

	public final String code;

	Language(String code) {
		this.code = code;
	}
}
