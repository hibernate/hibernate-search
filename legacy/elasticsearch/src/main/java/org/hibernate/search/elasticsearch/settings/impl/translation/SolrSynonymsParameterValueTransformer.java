/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import org.apache.lucene.analysis.util.ResourceLoader;

/**
 * @author Yoann Rodiere
 */
public class SolrSynonymsParameterValueTransformer extends AbstractSynonymsParameterValueTransformer {

	public SolrSynonymsParameterValueTransformer(ResourceLoader resourceLoader) {
		super( resourceLoader );
	}

	@Override
	protected String extractContent(String line) {
		/*
		 * See org.apache.lucene.analysis.synonym.SolrSynonymParser.addInternal(BufferedReader)::
		 * empty lines or comment lines ("^#.*") can exist, but inline comments are not supported
		 * (or so it seems).
		 */
		if ( line.length() == 0 || line.charAt( 0 ) == '#' ) {
			return null;
		}
		else {
			return line;
		}
	}

}
