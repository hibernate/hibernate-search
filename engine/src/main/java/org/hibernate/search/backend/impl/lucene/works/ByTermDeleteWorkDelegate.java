/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Extension of <code>DeleteLuceneWork</code> that will always perform the
 * delete LuceneWork in an optimal way, since the underlying data store guarantee
 * uniqueness of terms across different entity types.
 *
 * @author gustavonalle
 * @see {@link org.hibernate.search.backend.impl.lucene.works.DeleteWorkDelegate}
 */
public final class ByTermDeleteWorkDelegate extends DeleteWorkDelegate {

	private static final Log log = LoggerFactory.make();

	ByTermDeleteWorkDelegate(Workspace workspace) {
		super( workspace );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Class<?> managedType = work.getEntityClass();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		Serializable id = work.getId();
		log.tracef( "Removing %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( isIdNumeric( builder ) ) {
				writer.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
			}
			else {
				Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
				writer.deleteDocuments( idTerm );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}


}
