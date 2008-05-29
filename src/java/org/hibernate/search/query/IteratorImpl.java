//$Id$
package org.hibernate.search.query;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hibernate.search.engine.Loader;
import org.hibernate.search.engine.EntityInfo;

/**
 * @author Emmanuel Bernard
 */
//TODO load the next batch-size elements to benefit from batch-size 
public class IteratorImpl implements Iterator {

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
	public boolean hasNext() {
		if ( nextObjectIndex == index ) return next != null;
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

	public Object next() {
		//hasNext() has side effect
		if ( !hasNext() ) throw new NoSuchElementException( "Out of boundaries" );
		index++;
		return next;
	}

	public void remove() {
		//TODO this is theoretically doable
		throw new UnsupportedOperationException( "Cannot remove from a lucene query iterator" );
	}
}
