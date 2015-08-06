/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * @author Hardy Ferentschik
 */
public final class ISOLatin1Analyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		StandardTokenizer tokenizer = new StandardTokenizer();
		TokenStream filter = new ASCIIFoldingFilter( tokenizer );
		return new TokenStreamComponents( tokenizer, filter );
	}

}
