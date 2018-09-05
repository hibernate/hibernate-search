/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;

/**
 * @author Yoann Rodiere
 */
public class AbstractSynonymsParameterValueTransformer extends FileAsLineArrayParameterValueTransformer {

	public AbstractSynonymsParameterValueTransformer(ResourceLoader resourceLoader) {
		super( resourceLoader );
	}

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		/*
		 * See org.apache.lucene.analysis.synonym.SynonymFilterFactory.loadSynonyms(ResourceLoader, String, boolean, Analyzer)
		 * for instance.
		 */
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput( CodingErrorAction.REPORT )
				.onUnmappableCharacter( CodingErrorAction.REPORT );

		List<String> lines = new ArrayList<>();

		try ( final Reader reader = new InputStreamReader( stream, decoder );
				final LineNumberReader lineNumberReader = new LineNumberReader( reader ) ) {
			String line = lineNumberReader.readLine();
			while ( line != null ) {
				String content = extractContent( line );
				if ( content != null ) {
					lines.add( content );
				}
				line = lineNumberReader.readLine();
			}
		}

		return lines;
	}

	/**
	 * Extract the content from a line.
	 * <p>
	 * This method basically strips non-meaningful content, like comments, from a line.
	 *
	 * @param line The line to extract content from
	 * @return The content, or null if there is none.
	 */
	protected String extractContent(String line) {
		return line;
	}
}
