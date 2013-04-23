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
package org.hibernate.search.query.engine.spi;

import java.io.IOException;

import org.apache.lucene.search.TopDocs;

/**
 * DocumentExtractor is a traverser over the full-text results (EntityInfo)
 *
 * This operation is as lazy as possible:
 *  - the query is executed eagerly
 *  - results are not retrieved until actually requested
 *
 *  {@link #getFirstIndex()} and {@link #getMaxIndex()} define the boundaries available to {@link #extract(int)}.
 *
 * DocumentExtractor objects *must* be closed when the results are no longer traversed. See {@link #close()}
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface DocumentExtractor {
	EntityInfo extract(int index) throws IOException;

	int getFirstIndex();

	int getMaxIndex();

	void close();

	/**
	 * @experimental We are thinking at ways to encapsulate needs for exposing TopDocs (and whether or not it makes sense)
	 * Try to avoid it if you can
	 */
	TopDocs getTopDocs();
}
