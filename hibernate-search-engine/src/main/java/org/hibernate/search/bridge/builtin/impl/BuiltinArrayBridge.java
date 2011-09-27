/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.bridge.builtin.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.ArrayBridge;

/**
 * An implementation of {@link ArrayBridge} that can be used with Hibernate Search query DSL.
 *
 * @author Davide D'Alto
 */
public class BuiltinArrayBridge extends ArrayBridge implements StringBridge {

	private static final String2FieldBridgeAdaptor DEFAULT_STRING_BRIDGE = new String2FieldBridgeAdaptor( new DefaultStringBridge() );

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
