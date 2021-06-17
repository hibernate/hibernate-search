/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.logging;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

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

	private static final Escaper INSTANCE = Escapers.builder()
			.addEscape( '\u0000', "[\\u0000]" )
			.addEscape( '\uFFFF', "[\\uFFFF]" )
			.build();

	public static String escape(String string) {
		return string == null ? null : INSTANCE.escape( string );
	}

}
