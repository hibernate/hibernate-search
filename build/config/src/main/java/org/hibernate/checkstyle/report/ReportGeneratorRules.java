/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.report;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ReportGeneratorRules {
	private static final String INTERNAL_PREFIX = "-Rinternal ";
	private static final String PUBLIC_PREFIX = "-Rpublic ";
	private final Set<Pattern> publicRules;
	private final Set<Pattern> internalRules;

	ReportGeneratorRules(int start, String[] args) {
		Set<Pattern> internal = new HashSet<>();
		Set<Pattern> pub = new HashSet<>();
		for ( int index = start; index < args.length; index++ ) {
			String regex = args[index];
			if ( regex.startsWith( INTERNAL_PREFIX ) ) {
				internal.add( Pattern.compile( regex.substring( INTERNAL_PREFIX.length() ) ) );
			}
			else if ( regex.startsWith( PUBLIC_PREFIX ) ) {
				pub.add( Pattern.compile( regex.substring( PUBLIC_PREFIX.length() ) ) );
			}
			else {
				Pattern pattern = Pattern.compile( regex );
				internal.add( pattern );
				pub.add( pattern );
			}
		}
		publicRules = Collections.unmodifiableSet( pub );
		internalRules = Collections.unmodifiableSet( internal );
	}

	Optional<Pattern> matchAnyPublicRule(String path) {
		return matchAnyIgnoreRule( path, publicRules );
	}

	Optional<Pattern> matchAnyInternalRule(String path) {
		return matchAnyIgnoreRule( path, internalRules );
	}

	private Optional<Pattern> matchAnyIgnoreRule(String path, Set<Pattern> rules) {
		for ( Pattern rule : rules ) {
			if ( rule.matcher( path ).matches() ) {
				return Optional.of( rule );
			}
		}
		return Optional.empty();
	}
}
