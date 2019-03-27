/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.util.regex.Pattern;

class AwsNormalization {

	private AwsNormalization() {
		// Private, this is a utils class
	}

	private static final Pattern HOST_PORT_REGEX = Pattern.compile( ":\\d+$" );

	static String normalizeHost(String value) {
		return HOST_PORT_REGEX.matcher( value ).replaceAll( "" );
	}

}
