/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.logging;

import java.util.Map;

/**
 * Escapers to work around various bugs in test tools.
 * <p>
 * In particular, whatever produces the XML test reports (Maven Surefire? JUnit?)
 * does not properly escapes the character \uFFFF,
 * which results in parsing errors in Jenkins.
 * So we have to escape the character ourselves.
 */
public final class TestEscapers {

	private TestEscapers() {
	}

	private static final Map<String, String> ESCAPE = Map.of(
			"\u0000", "[\\u0000]",
			"\uFFFF", "[\\uFFFF]"
	);

	public static String escape(String string) {
		if ( string == null ) {
			return null;
		}
		for ( var entry : ESCAPE.entrySet() ) {
			string = string.replace( entry.getKey(), entry.getValue() );
		}
		return string;
	}

}
