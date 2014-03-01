/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.BytesRef;

/**
 * A {@code FieldLoadingStrategy} which uses {@link SortedSetDocValues} to load fields with multiple values.
 *
 * @author Hardy Ferentschik
 */
public final class MultiStringFieldLoadingStrategy implements FieldLoadingStrategy {

	private final String fieldName;
	private SortedSetDocValues sortedSetDocValues;

	public MultiStringFieldLoadingStrategy(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void loadNewCacheValues(AtomicReaderContext context) throws IOException {
		// FieldCache.getDocTermOrd allows to get all terms for a given multi value field per document. SortedSetDocValues
		// allows to retrieve the term values via iterator like API
		// See also https://issues.apache.org/jira/browse/LUCENE-3354
		sortedSetDocValues = FieldCache.DEFAULT.getDocTermOrds( context.reader(), fieldName );
	}

	@Override
	public String[] collect(int relativeDocId) {
		// use the loaded SortedSetDocValues to retrieve all values for the field
		sortedSetDocValues.setDocument( relativeDocId );
		List<String> values = new ArrayList<String>();

		BytesRef bytesRef = new BytesRef();
		long ordinal = sortedSetDocValues.nextOrd();
		while ( ordinal != SortedSetDocValues.NO_MORE_ORDS ) {
			sortedSetDocValues.lookupOrd( ordinal, bytesRef );
			values.add( bytesRef.utf8ToString() );
			ordinal = sortedSetDocValues.nextOrd();
		}

		return values.toArray( new String[values.size()] );
	}

}
