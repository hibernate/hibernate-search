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

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.WordlistLoader;

/**
 * @author Yoann Rodiere
 */
public class WordSetFileParameterValueTransformer extends FileAsLineArrayParameterValueTransformer {

	public WordSetFileParameterValueTransformer(ResourceLoader resourceLoader) {
		super( resourceLoader );
	}

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		/*
		 * See org.apache.lucene.analysis.util.AbstractAnalysisFactory.getWordSet(ResourceLoader, String, boolean):
		 * it simply uses org.apache.lucene.analysis.util.WordlistLoader.getLines(InputStream, Charset)
		 * to retrieve the words ("wlist").
		 */
		return WordlistLoader.getLines( stream, StandardCharsets.UTF_8 );
	}

}
