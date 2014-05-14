/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Wraps a FieldCacheCollector in such a way that {@link #getValue(int)} returns objects as transformed
 * by a {@link org.hibernate.search.bridge.TwoWayStringBridge} to transform from String form to the object.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TwoWayTransformingFieldCacheCollector extends FieldCacheCollector {

	private static final Log log = LoggerFactory.make();

	private final TwoWayStringBridge stringBridge;
	private final FieldCacheCollector privateDelegate;

	/**
	 * @param delegate Actually uses the delegate Collector implementation
	 * @param twoWayStringBridge Converts returned values using this {@link org.hibernate.search.bridge.TwoWayStringBridge#stringToObject(String)}
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
	public Object getValue(int docId) {
		String value = (String) privateDelegate.getValue( docId );
		if ( value == null ) {
			log.unexpectedValueMissingFromFieldCache();
			return null;
		}
		return stringBridge.stringToObject( value );
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		delegate.setNextReader( context );
	}

}
