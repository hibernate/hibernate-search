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

import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs a <code>DeleteLuceneWork</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class DeleteWorkDelegate implements LuceneWorkDelegate {

	private static final Log log = LoggerFactory.make();	
	protected final Workspace workspace;

	DeleteWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		final Serializable id = work.getId();
		log.tracef( "Removing %s#%s by query.", entityType, id );
		DocumentBuilderIndexedEntity<?> builder = workspace.getDocumentBuilder( entityType );

		BooleanQuery entityDeletionQuery = new BooleanQuery();

		Query idQueryTerm;
		if ( isIdNumeric( builder ) ) {
			idQueryTerm = NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id );
		} else {
			Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
			idQueryTerm = new TermQuery( idTerm );
		}
		entityDeletionQuery.add( idQueryTerm, BooleanClause.Occur.MUST );

		Term classNameQueryTerm =  new Term( ProjectionConstants.OBJECT_CLASS, entityType.getName() );
		TermQuery classNameQuery = new TermQuery( classNameQueryTerm );
		entityDeletionQuery.add( classNameQuery, BooleanClause.Occur.MUST );

		try {
			writer.deleteDocuments( entityDeletionQuery );
		}
		catch ( Exception e ) {
			String message = "Unable to remove " + entityType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
		workspace.incrementModificationCounter( 1 );
	}

	protected static boolean isIdNumeric(DocumentBuilderIndexedEntity<?> documentBuilder) {
		TwoWayFieldBridge idBridge = documentBuilder.getIdBridge();
		return idBridge instanceof NumericFieldBridge;
	}

}
