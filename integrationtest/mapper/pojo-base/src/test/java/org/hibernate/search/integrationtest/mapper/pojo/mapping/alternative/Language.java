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
