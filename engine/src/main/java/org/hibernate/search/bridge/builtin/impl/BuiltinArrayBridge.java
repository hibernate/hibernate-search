/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.ArrayBridge;
import org.hibernate.search.bridge.builtin.DefaultStringBridge;

/**
 * An implementation of {@link org.hibernate.search.bridge.builtin.ArrayBridge} that can be used with Hibernate Search query DSL.
 *
 * @author Davide D'Alto
 */
public class BuiltinArrayBridge extends ArrayBridge implements StringBridge {

	private static final String2FieldBridgeAdaptor DEFAULT_STRING_BRIDGE = new String2FieldBridgeAdaptor( DefaultStringBridge.INSTANCE );

	private final StringBridge bridge;

	public BuiltinArrayBridge() {
		this( DEFAULT_STRING_BRIDGE );
	}

	public BuiltinArrayBridge(final FieldBridge fieldBridge) {
		super( fieldBridge );
		if ( fieldBridge instanceof StringBridge ) {
			this.bridge = (StringBridge) fieldBridge;
		}
		else {
			this.bridge = DEFAULT_STRING_BRIDGE;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.search.bridge.StringBridge#objectToString(java.lang.Object)
	 */
	@Override
	public String objectToString(Object object) {
		return bridge.objectToString( object );
	}

	@Override
	public String toString() {
		return "BuiltinArrayBridge{" +
				"bridge=" + bridge +
				'}';
	}
}
