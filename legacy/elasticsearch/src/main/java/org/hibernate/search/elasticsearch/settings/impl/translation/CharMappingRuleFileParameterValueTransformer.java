/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.hibernate.search.exception.SearchException;

/**
 * @author Yoann Rodiere
 */
public class CharMappingRuleFileParameterValueTransformer extends FileAsLineArrayParameterValueTransformer {
	private static final Pattern PATTERN = Pattern.compile( "\"(.*)\"\\s*=>\\s*\"(.*)\"\\s*$" );

	public CharMappingRuleFileParameterValueTransformer(ResourceLoader resourceLoader) {
		super( resourceLoader );
	}

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		// See org.apache.lucene.analysis.charfilter.MappingCharFilterFactory.inform(ResourceLoader)
		List<String> lines = WordlistLoader.getLines( stream, StandardCharsets.UTF_8 );

		ListIterator<String> it = lines.listIterator();
		while ( it.hasNext() ) {
			String line = it.next();
			Matcher matcher = PATTERN.matcher( line );
			if ( matcher.matches() ) {
				// Remove double quotes: Elasticsearch doesn't expect those
				line = matcher.group( 1 ) + " => " + matcher.group( 2 );
				it.set( line );
			}
			else {
				throw new SearchException( "Invalid rule syntax: " + line );
			}
		}

		return lines;
	}

}
