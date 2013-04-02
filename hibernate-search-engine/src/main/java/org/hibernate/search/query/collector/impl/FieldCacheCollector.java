/*
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

package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Because Lucene's Collector is not an interface, we have to create extensions of it.
 * All our implementations need a {@link #getValue(int)}, so we need an
 * abstract superclass defining it.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public abstract class FieldCacheCollector extends Collector {
	protected final Collector delegate;
	private final boolean acceptsDocsOutOfOrder;

	public FieldCacheCollector(Collector delegate) {
		this.delegate = delegate;
		this.acceptsDocsOutOfOrder = delegate.acceptsDocsOutOfOrder();
	}

	@Override
	public final void setScorer(Scorer scorer) throws IOException {
		this.delegate.setScorer( scorer );
	}

	@Override
	public final boolean acceptsDocsOutOfOrder() {
		return acceptsDocsOutOfOrder;
	}

	@Override
	public abstract void collect(int doc) throws IOException;

	@Override
	public abstract void setNextReader(IndexReader reader, int docBase) throws IOException;

	public abstract Object getValue(int docId);
}
