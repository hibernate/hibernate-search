/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;

/**
 * Extension of {@link ByTermUpdateWorkExecutor} bound to a single entity.
 * <p>
 * This allows not retrieving the document builder for each document.
 * <p>
 * NOTE (yrodiere): This is of dubious interest performance-wise, but kept as-is not to risk regressions.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public final class UpdateExtWorkExecutor extends ByTermUpdateWorkExecutor {

	private final IndexedTypeIdentifier managedType;
	private final DocumentBuilderIndexedEntity builder;
	private final boolean idIsNumeric;

	UpdateExtWorkExecutor(Workspace workspace, AddWorkExecutor addDelegate) {
		super( workspace, addDelegate );
		this.managedType = workspace.getEntitiesInIndexManager().iterator().next();
		this.builder = workspace.getDocumentBuilder( managedType );
		this.idIsNumeric = DeleteWorkExecutor.isIdNumeric( builder );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		checkType( work );
		doPerformWork( work, delegate, monitor, managedType, builder, idIsNumeric );
	}

	private void checkType(final LuceneWork work) {
		if ( ! work.getEntityType().equals( managedType ) ) {
			throw new AssertionFailure( "Unexpected type: " + work.getEntityType() + " This workspace expects: " + managedType );
		}
	}

}
