/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport.data;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.hibernate.annotations.Immutable;

@Immutable
public class ISBN implements Serializable {

	private static final Pattern ISBN_13_PATTERN = Pattern.compile( "\\d{3}-\\d-\\d{2}-\\d{6}-\\d" );

	public static ISBN parse(String string) {
		if ( string == null || !ISBN_13_PATTERN.matcher( string ).matches() ) {
			throw new IllegalArgumentException( "Not an ISBN: " + string );
		}
		return new ISBN( string );
	}

	private final String stringValue;

	private ISBN(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ISBN && stringValue.equals( ( (ISBN) obj ).stringValue );
	}

	@Override
	public int hashCode() {
		return stringValue.hashCode();
	}
}
