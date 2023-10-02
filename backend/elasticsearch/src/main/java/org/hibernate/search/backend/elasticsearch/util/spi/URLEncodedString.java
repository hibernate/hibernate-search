/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.util.spi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * To avoid re-encoding strings into URLs we have several opportunities
 * to reuse them and compose them more efficiently.
 */
public final class URLEncodedString {

	public final String encoded;
	public final String original;

	/**
	 * Do not invoke directly.
	 * @see #fromString(String)
	 * @param string the original string to be encoded.
	 */
	private URLEncodedString(String string) {
		this.original = string;
		try {
			encoded = URLEncoder.encode( string, StandardCharsets.UTF_8.name() );
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionFailure( "Unexpected error retrieving the UTF-8 charset", e );
		}
	}

	@Override
	public String toString() {
		return original;
	}

	@Override
	public int hashCode() {
		return original.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null ) {
			return false;
		}
		else if ( URLEncodedString.class != obj.getClass() ) {
			return false;
		}
		else {
			URLEncodedString other = (URLEncodedString) obj;
			return original.equals( other.original );
		}
	}

	public static URLEncodedString fromString(String string) {
		Contracts.assertNotNull( string, "string" );
		return new URLEncodedString( string );
	}

}
