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
package org.hibernate.search.backend.impl;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Policy for committing changesets.
 *
 * Implementations of this interface will decide
 * the commit strategy based of three backend related events: after a changeset is applied,
 * when an explicit flush is requested and when backend closing
 *
 * @author gustavonalle
 */
public interface CommitPolicy {

	/**
	 * A changeset was applied to the index
	 * @param someFailureHappened true if any failure happened
	 * @param streaming true if changesets are part of a stream of operations
	 */
	void onChangeSetApplied(boolean someFailureHappened, boolean streaming);

	/**
	 * An explicit flush was requested
	 */
	void onFlush();

	/**
	 * Backend shutting down
	 */
	void onClose();

	/**
	 * Obtain the IndexWriter
	 */
	IndexWriter getIndexWriter();

	/**
	 * Obtain the index writer
	 */
	IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder);

}
