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

package org.hibernate.search.bridge.builtin.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.IterableBridge;

/**
 * An implementation of {@link org.hibernate.search.bridge.builtin.IterableBridge} that can be used with Hibernate Search query DSL.
 *
 * @author Davide D'Alto
 */
public class BuiltinIterableBridge extends IterableBridge implements StringBridge {

	private static final String2FieldBridgeAdaptor DEFAULT_STRING_BRIDGE = new String2FieldBridgeAdaptor( DefaultStringBridge.INSTANCE );

	private final StringBridge bridge;

	public BuiltinIterableBridge() {
		this( DEFAULT_STRING_BRIDGE );
	}

	public BuiltinIterableBridge(final FieldBridge fieldBridge) {
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
