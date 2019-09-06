/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchVersion {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Pattern pattern = Pattern.compile( "(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:-(\\w+))?)?)?" );

	/**
	 * @param versionString A version string following the format {@code x.y.z-qualifier},
	 * where {@code x}, {@code y} and {@code z} are integers and {@code qualifier} is a string of word characters (alphanumeric or '_').
	 * Incomplete versions are allowed, for example {@code 7.0} or just {@code 7}.
	 * @return An {@link ElasticsearchVersion} object representing the given version.
	 * @throws org.hibernate.search.util.common.SearchException If the input string doesn't follow the required format.
	 */
	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchVersion of(String versionString) {
		final String normalizedVersion = versionString.trim().toLowerCase( Locale.ROOT );
		Matcher matcher = pattern.matcher( normalizedVersion );
		if ( !matcher.matches() ) {
			throw log.invalidElasticsearchVersion( normalizedVersion );
		}
		String major = matcher.group( 1 );
		String minor = matcher.group( 2 );
		String micro = matcher.group( 3 );
		String qualifier = matcher.group( 4 );
		return new ElasticsearchVersion(
				Integer.parseInt( major ),
				minor == null ? null : Integer.parseInt( minor ),
				micro == null ? null : Integer.parseInt( micro ),
				qualifier
		);
	}

	private final int major;
	private final Integer minor;
	private final Integer micro;
	private final String qualifier;

	private ElasticsearchVersion(int major, Integer minor, Integer micro, String qualifier) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.qualifier = qualifier;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( major );
		if ( minor != null ) {
			builder.append( "." ).append( minor );
		}
		if ( micro != null ) {
			builder.append( "." ).append( micro );
		}
		if ( qualifier != null ) {
			builder.append( "-" ).append( qualifier );
		}
		return builder.toString();
	}

	public int getMajor() {
		return major;
	}

	public OptionalInt getMinor() {
		return minor == null ? OptionalInt.empty() : OptionalInt.of( minor );
	}

	public OptionalInt getMicro() {
		return micro == null ? OptionalInt.empty() : OptionalInt.of( micro );
	}

	public Optional<String> getQualifier() {
		return Optional.ofNullable( qualifier );
	}

	/**
	 * @param other A version to be matched against this version.
	 * @return {@code true} if the other version matches this version,
	 * i.e. if all the components defined in this version are also defined in the other version with the same value.
	 * {@code false} otherwise.
	 * Components that are not defined in this version do not matter.
	 */
	public boolean matches(ElasticsearchVersion other) {
		return major == other.major
				&& ( minor == null || minor.equals( other.minor ) )
				&& ( micro == null || micro.equals( other.micro ) )
				&& ( qualifier == null || qualifier.equals( other.qualifier ) );
	}
}
