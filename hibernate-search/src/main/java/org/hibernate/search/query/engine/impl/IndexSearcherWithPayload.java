/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.query.engine.impl;

import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.util.LoggerFactory;

import static org.hibernate.search.reader.ReaderProviderHelper.getIndexReaders;

/**
* @author Emmanuel Bernard
*/
public class IndexSearcherWithPayload {
	private static final Logger log = LoggerFactory.make();
	private final IndexSearcher searcher;
	private boolean fieldSortDoTrackScores;
	private boolean fieldSortDoMaxScore;

	public IndexSearcherWithPayload(IndexSearcher searcher, boolean fieldSortDoTrackScores, boolean fieldSortDoMaxScore) {
		this.searcher = searcher;
		this.fieldSortDoTrackScores = fieldSortDoTrackScores;
		this.fieldSortDoMaxScore = fieldSortDoMaxScore;
		searcher.setDefaultFieldSortScoring( fieldSortDoTrackScores, fieldSortDoMaxScore );
	}

	public IndexSearcher getSearcher() {
		return searcher;
	}

	public boolean isFieldSortDoTrackScores() {
		return fieldSortDoTrackScores;
	}

	public boolean isFieldSortDoMaxScore() {
		return fieldSortDoMaxScore;
	}

	/**
	 * @param query toString() is invoked to display the query in the warning message
	 * @param searchFactoryImplementor
	 */
	public void closeSearcher(Object query, SearchFactoryImplementor searchFactoryImplementor) {
		Set<IndexReader> indexReaders = getIndexReaders( getSearcher() );
		ReaderProvider readerProvider = searchFactoryImplementor.getReaderProvider();
		for ( IndexReader indexReader : indexReaders ) {
			try {
				readerProvider.closeReader( indexReader );
			}
			catch (SearchException e) {
				//catch is inside the for loop to make sure we try to close all of them
				log.warn( "Unable to properly close searcher during lucene query: " + query.toString(), e );
			}
		}
	}
}
