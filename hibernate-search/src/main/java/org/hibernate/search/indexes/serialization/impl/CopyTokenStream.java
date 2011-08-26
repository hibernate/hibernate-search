/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CopyTokenStream extends TokenStream implements Serializable {
	private static final Log log = LoggerFactory.make();

	private List<List<AttributeImpl>> cache;
	private int index;

	public static SerializableTokenStream buildSerializabletokenStream(TokenStream tokenStream) {
		try {
			List<List<AttributeImpl>> stream = fillCache( tokenStream );
			return new SerializableTokenStream(stream);
		}
		catch ( IOException e ) {
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
		setState(index);
		index++;
		return true;
	}

	private void setState(int localIndex) {
		for ( AttributeImpl attr : cache.get(localIndex) ) {
			addAttributeImpl( attr );
		}
	}

	@Override
	public final void end() throws IOException {
		if (cache.size() > 0) {
			setState(cache.size()-1);
		}
	}

	@Override
	public void reset() throws IOException {
		index = 0;
	}

	private static List<List<AttributeImpl>> fillCache(TokenStream input) throws IOException {
		List<List<AttributeImpl>> results = new ArrayList<List<AttributeImpl>>();
		while ( input.incrementToken() ) {
			List<AttributeImpl> attrs = new ArrayList<AttributeImpl>(  );
			results.add( attrs );
			Iterator<AttributeImpl> iter = input.getAttributeImplsIterator();
			while ( iter.hasNext() ) {
				//we need to clone as AttributeImpl instances can be reused across incrementToken() calls
				attrs.add( (AttributeImpl) iter.next().clone() );
			}
		}
		input.end();
		return results;
	}
}
