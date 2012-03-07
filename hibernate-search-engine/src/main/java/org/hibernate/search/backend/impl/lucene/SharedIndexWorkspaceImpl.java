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
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class SharedIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	private final Object lock = new Object();
	private int openWriterUsers = 0;
	private boolean lastExitCloses = false;

	public SharedIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		synchronized ( lock ) {
			openWriterUsers--;
			if ( openWriterUsers == 0 ) {
				if ( someFailureHappened ) {
					writerHolder.forceLockRelease();
				}
				else {
					if ( ! streaming || lastExitCloses ) {
						lastExitCloses = false;
						writerHolder.closeIndexWriter();
					}
				}
			}
			else {
				if ( ! someFailureHappened && ! streaming ) {
					writerHolder.commitIndexWriter();
				}
			}
		}
	}

	@Override
	public IndexWriter getIndexWriter() {
		synchronized ( lock ) {
			openWriterUsers++;
			return super.getIndexWriter();
		}
	}

	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		synchronized ( lock ) {
			openWriterUsers++;
			return super.getIndexWriter( errorContextBuilder );
		}
	}

	@Override
	public void flush() {
		synchronized ( lock ) {
			if ( openWriterUsers == 0 ) {
				writerHolder.closeIndexWriter();
			}
			else {
				lastExitCloses = true;
				writerHolder.commitIndexWriter();
			}
		}
	}

}
