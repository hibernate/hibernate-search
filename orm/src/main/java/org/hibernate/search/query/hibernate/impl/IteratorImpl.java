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
package org.hibernate.search.query.hibernate.impl;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hibernate.search.query.engine.spi.EntityInfo;

/**
 * @author Emmanuel Bernard
 */
//TODO load the next batch-size elements to benefit from batch-size
public class IteratorImpl implements Iterator<Object> {

	private final List<EntityInfo> entityInfos;
	private int index = 0;
	private final int size;
	private Object next;
	private int nextObjectIndex = -1;
	private final Loader loader;

	public IteratorImpl(List<EntityInfo> entityInfos, Loader loader) {
		this.entityInfos = entityInfos;
		this.size = entityInfos.size();
		this.loader = loader;
	}

	//side effect is to set up next
	@Override
	public boolean hasNext() {
		if ( nextObjectIndex == index ) {
			return next != null;
		}
		next = null;
		nextObjectIndex = -1;
		do {
			if ( index >= size ) {
				nextObjectIndex = index;
				next = null;
				return false;
			}
			next = loader.load( entityInfos.get( index ) );
			if ( next == null ) {
				index++;
			}
			else {
				nextObjectIndex = index;
			}
		}
		while ( next == null );
		return true;
	}

	@Override
	public Object next() {
		//hasNext() has side effect
		if ( !hasNext() ) {
			throw new NoSuchElementException( "Out of boundaries" );
		}
		index++;
		return next;
	}

	@Override
	public void remove() {
		//TODO this is theoretically doable
		throw new UnsupportedOperationException( "Cannot remove from a lucene query iterator" );
	}
}
