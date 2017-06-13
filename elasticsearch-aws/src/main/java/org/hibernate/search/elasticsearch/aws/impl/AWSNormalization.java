/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.aws.impl;

/**
 * @author Yoann Rodiere
 */
class AWSNormalization {

	private AWSNormalization() {
		// Private, this is a utils class
	}

	private static final String HOST_PORT_REGEX = ":\\d+$";

	public static String normalizeHost(String value) {
		return value.replaceAll( HOST_PORT_REGEX, "" );
	}

}
