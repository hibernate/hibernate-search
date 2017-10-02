/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.definition;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * @author Hardy Ferentschik
 */
public class InsertWhitespaceFilterFactory extends TokenFilterFactory {

	public InsertWhitespaceFilterFactory(Map<String, String> args) {
		super( args );
	}

	@Override
	public InsertWhitespaceFilter create(TokenStream input) {
		return new InsertWhitespaceFilter( input );
	}
}
