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
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs an <code>AddLuceneWork</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class AddWorkDelegate implements LuceneWorkDelegate {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	AddWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		DocumentBuilderIndexedEntity<?> documentBuilder = workspace.getDocumentBuilder( entityType );
		Map<String, String> fieldToAnalyzerMap = work.getFieldToAnalyzerMap();
		ScopedAnalyzer analyzer = documentBuilder.getAnalyzer();
		analyzer = updateAnalyzerMappings( workspace, analyzer, fieldToAnalyzerMap );
		if ( log.isTraceEnabled() ) {
			log.trace( "add to Lucene index: " + entityType + "#" + work.getId() + ":" + work.getDocument() );
		}
		try {
			writer.addDocument( work.getDocument(), analyzer );
			workspace.incrementModificationCounter( 1 );
		}
		catch ( IOException e ) {
			throw new SearchException(
					"Unable to add to Lucene index: "
							+ entityType + "#" + work.getId(), e
			);
		}
		if ( monitor != null ) {
			monitor.documentsAdded( 1l );
		}
	}

	/**
	 * Allows to override the otherwise static field to analyzer mapping in <code>scopedAnalyzer</code>.
	 *
	 * @param workspace The current work context
	 * @param scopedAnalyzer The scoped analyzer created at startup time.
	 * @param fieldToAnalyzerMap A map of <code>Document</code> field names for analyzer names. This map gets creates
	 * when the Lucene <code>Document</code> gets created and uses the state of the entity to index to determine analyzers
	 * dynamically at index time.
	 *
	 * @return <code>scopedAnalyzer</code> in case <code>fieldToAnalyzerMap</code> is <code>null</code> or empty. Otherwise
	 *         a clone of <code>scopedAnalyzer</code> is created where the analyzers get overriden according to <code>fieldToAnalyzerMap</code>.
	 */
	static ScopedAnalyzer updateAnalyzerMappings(Workspace workspace, ScopedAnalyzer scopedAnalyzer, Map<String, String> fieldToAnalyzerMap) {
		// for backwards compatibility
		if ( fieldToAnalyzerMap == null || fieldToAnalyzerMap.isEmpty() ) {
			return scopedAnalyzer;
		}

		ScopedAnalyzer analyzerClone = scopedAnalyzer.clone();
		for ( Map.Entry<String, String> entry : fieldToAnalyzerMap.entrySet() ) {
			Analyzer analyzer = workspace.getAnalyzer( entry.getValue() );
			if ( analyzer == null ) {
				log.unableToRetrieveNamedAnalyzer( entry.getValue() );
			}
			else {
				analyzerClone.addScopedAnalyzer( entry.getKey(), analyzer );
			}
		}
		return analyzerClone;
	}

}
