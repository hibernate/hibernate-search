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

package org.hibernate.search.bridge.builtin.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.ArrayBridge;

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

}
