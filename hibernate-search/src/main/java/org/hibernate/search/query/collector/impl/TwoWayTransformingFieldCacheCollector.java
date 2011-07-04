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
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Wraps a FieldCacheCollector in such a way that {@link #getValue(int)} returns objects as transformed
 * by a {@link TwoWayStringBridge} to transform from String form to the object.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TwoWayTransformingFieldCacheCollector extends FieldCacheCollector {
	
	private static final Log log = LoggerFactory.make();
	
	private final TwoWayStringBridge stringBridge;
	private final FieldCacheCollector privateDelegate;

	/**
	 * @param delegate Actually uses the delegate Collector implementation
	 * @param twoWayStringBridge Converts returned values using this {@link TwoWayStringBridge#stringToObject(String)}
	 */
	public TwoWayTransformingFieldCacheCollector(FieldCacheCollector delegate, TwoWayStringBridge twoWayStringBridge) {
		super( delegate );
		this.privateDelegate = delegate;
		this.stringBridge = twoWayStringBridge;
	}

	@Override
	public void collect(int doc) throws IOException {
		delegate.collect( doc );
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		delegate.setNextReader( reader, docBase );
	}

	@Override
	public Object getValue(int docId) {
		String value = (String) privateDelegate.getValue( docId );
		if ( value == null ) {
			log.unexpectedValueMissingFromFieldCache();
			return null;
		}
		return stringBridge.stringToObject( value );
	}

}
