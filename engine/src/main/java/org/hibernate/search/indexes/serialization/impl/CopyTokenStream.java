/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.AttributeImpl;

import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Copy all AttributeImpl for each incrementToken
 *
 * Inspired by org.apache.lucene.analysis.CachingTokenFilter
 * Original file released under the ASL 2.0 license
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * @author Emmanuel Bernard
 */
public class CopyTokenStream extends TokenStream implements Serializable {
	private static final Log log = LoggerFactory.make();

	private List<List<AttributeImpl>> cache;
	private int index;

	public static SerializableTokenStream buildSerializableTokenStream(TokenStream tokenStream) {
		try {
			List<List<AttributeImpl>> stream = createAttributeLists( tokenStream );
			return new SerializableTokenStream(stream);
		}
		catch (IOException e) {
			throw log.unableToReadTokenStream();
		}
	}

	public CopyTokenStream(List<List<AttributeImpl>> stream) {
		this.index = 0;
		this.cache = stream;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if ( index >= cache.size() ) {
			// the cache is exhausted, return false
			return false;
		}

		// Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
		setState( index );
		index++;
		return true;
	}

	private void setState(int localIndex) {
		for ( AttributeImpl attr : cache.get( localIndex ) ) {
			addAttributeImpl( attr );
		}
	}

	@Override
	public final void end() throws IOException {
		if ( cache.size() > 0 ) {
			setState( cache.size() - 1 );
		}
	}

	@Override
	public void reset() throws IOException {
		index = 0;
	}

	private static List<List<AttributeImpl>> createAttributeLists(TokenStream input) throws IOException {
		List<List<AttributeImpl>> results = new ArrayList<>();
		while ( input.incrementToken() ) {
			List<AttributeImpl> attrs = new ArrayList<>();
			results.add( attrs );
			Iterator<AttributeImpl> iter = input.getAttributeImplsIterator();
			while ( iter.hasNext() ) {
				//we need to clone as AttributeImpl instances can be reused across incrementToken() calls
				attrs.add( iter.next().clone() );
			}
		}
		input.end();
		return results;
	}
}
