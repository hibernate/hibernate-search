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
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.bridge.util.NumericFieldUtils;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Extension of <code>DeleteLuceneWork</code> bound to a single entity.
 * This allows to perform the delete LuceneWork in an optimal way in case
 * the index is NOT shared across different entities (which is the default).
 *
 * @author Sanne Grinovero
 * @see DeleteWorkDelegate
 */
public class DeleteExtWorkDelegate extends DeleteWorkDelegate {

	private final Class<?> managedType;
	private final DocumentBuilderIndexedEntity<?> builder;
	private final Logger log = LoggerFactory.make();
	private final boolean idIsNumeric;

	DeleteExtWorkDelegate(Workspace workspace, WorkerBuildContext context) {
		super( workspace );
		managedType = workspace.getEntitiesInDirectory().iterator().next();
		builder = context.getDocumentBuilderIndexedEntity( managedType );
		idIsNumeric = isIdNumeric( managedType, builder );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer) {
		checkType( work );
		Serializable id = work.getId();
		log.trace( "Removing {}#{} by id using an IndexWriter.", managedType, id );
		try {
			if( idIsNumeric ) {
				writer.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
			} else {
				Term idTerm = builder.getTerm( id );
				writer.deleteDocuments( idTerm );
			}
		}
		catch ( Exception e ) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}

	private void checkType(final LuceneWork work) {
		if ( work.getEntityClass() != managedType ) {
			throw new AssertionFailure( "Unexpected type" );
		}
	}

}
