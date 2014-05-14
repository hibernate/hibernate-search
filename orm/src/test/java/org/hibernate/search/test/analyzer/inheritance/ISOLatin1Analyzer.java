/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Hardy Ferentschik
 */
public final class ISOLatin1Analyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		StandardTokenizer tokenizer = new StandardTokenizer( TestConstants.getTargetLuceneVersion(), reader );
		TokenStream filter = new ASCIIFoldingFilter( tokenizer );
		return new TokenStreamComponents( tokenizer, filter );
	}

}
