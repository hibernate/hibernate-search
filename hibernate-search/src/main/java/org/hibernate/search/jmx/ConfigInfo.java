/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Version;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.util.ReflectHelper;

/**
 * Implementation of the {@code ConfigInfoMBean} JMX attributes and operations.
 *
 * @author Hardy Ferentschik
 */
public class ConfigInfo implements ConfigInfoMBean {

	private final SearchFactoryImplementor searchFactoryImplementor;

	public ConfigInfo(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public String getSearchVersion() {
		return Version.getVersionString();
	}

	public Set<String> getIndexedClassNames() {
		Set<String> indexedClasses = new HashSet<String>();
		for ( Class clazz : searchFactoryImplementor.getDocumentBuildersIndexedEntities().keySet() ) {
			indexedClasses.add( clazz.getName() );
		}
		return indexedClasses;
	}

	public String getIndexingStrategy() {
		return searchFactoryImplementor.getIndexingStrategy();
	}

	public int getNumberOfIndexedEntities(String entity) {
		Class<?> clazz = getEntityClass( entity );
		DirectoryProvider[] directoryProviders = searchFactoryImplementor.getDirectoryProviders( clazz );
		ReaderProvider readerProvider = searchFactoryImplementor.getReaderProvider();

		int count = 0;
		for ( DirectoryProvider directoryProvider : directoryProviders ) {
			IndexReader reader = readerProvider.openReader( directoryProvider );
			IndexSearcher searcher = new IndexSearcher( reader );
			BooleanQuery boolQuery = new BooleanQuery();
			boolQuery.add( new MatchAllDocsQuery(), BooleanClause.Occur.MUST );
			boolQuery.add(
					new TermQuery( new Term( ProjectionConstants.OBJECT_CLASS, entity ) ), BooleanClause.Occur.MUST
			);
			try {
				TopDocs topdocs = searcher.search( boolQuery, 1 );
				count += topdocs.totalHits;
			}
			catch ( IOException e ) {
				throw new RuntimeException( "Unable to execute count query for entity " + entity, e );
			}
			finally {
				readerProvider.closeReader( reader );
			}
		}
		return count;
	}

	public Map<String, Integer> indexedEntitiesCount() {
		Map<String, Integer> countPerEntity = new HashMap<String, Integer>();
		for ( String className : getIndexedClassNames() ) {
			countPerEntity.put( className, getNumberOfIndexedEntities( className ) );
		}
		return countPerEntity;
	}

	public List<String> getIndexingParameters(String entity) {
		Class<?> clazz = getEntityClass( entity );
		List<String> indexingParameters = new ArrayList<String>();
		for ( DirectoryProvider directoryProvider : searchFactoryImplementor.getDirectoryProviders( clazz ) ) {
			indexingParameters.add( searchFactoryImplementor.getIndexingParameters( directoryProvider ).toString() );
		}
		return indexingParameters;
	}

	private Class<?> getEntityClass(String entity) {
		Class<?> clazz;
		try {
			clazz = ReflectHelper.classForName( entity, ConfigInfo.class );
		}
		catch ( ClassNotFoundException e ) {
			throw new IllegalArgumentException( entity + "not a indexed entity" );
		}
		return clazz;
	}
}


