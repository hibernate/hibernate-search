/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchVersion {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Pattern pattern = Pattern.compile( "(\\d+)\\.(\\d+)\\.(\\d+)" );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchVersion of(String propertyValue) {
		final String normalizedVersion = propertyValue.toString().trim().toLowerCase( Locale.ROOT );
		Matcher matcher = pattern.matcher( normalizedVersion );
		if ( !matcher.matches() ) {
			throw log.invalidElasticsearchVersion( normalizedVersion );
		}
		String major = matcher.group( 1 );
		String minor = matcher.group( 2 );
		String micro = matcher.group( 3 );
		return new ElasticsearchVersion(
				Integer.parseInt( major ), Integer.parseInt( minor ), Integer.parseInt( micro )
		);
	}

	private final int major;
	private final int minor;
	private final int micro;

	private ElasticsearchVersion(int major, int minor, int micro) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + micro;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getMicro() {
		return micro;
	}
}
