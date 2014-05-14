/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

/**
 * Bridge to use a StringBridge as a FieldBridge.
 *
 * @author Emmanuel Bernard (C) 2011 Red Hat Inc.
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class String2FieldBridgeAdaptor implements FieldBridge, StringBridge, AppliedOnTypeAwareBridge {
	private final StringBridge stringBridge;

	public String2FieldBridgeAdaptor(StringBridge stringBridge) {
		this.stringBridge = stringBridge;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String indexedString = stringBridge.objectToString( value );
		if ( indexedString == null && luceneOptions.indexNullAs() != null ) {
			indexedString = luceneOptions.indexNullAs();
		}
		luceneOptions.addFieldToDocument( name, indexedString, document );
	}

	@Override
	public String objectToString(Object object) {
		return stringBridge.objectToString( object );
	}

	@Override
	public String toString() {
		return "String2FieldBridgeAdaptor [stringBridge=" + stringBridge + "]";
	}

	@Override
	public void setAppliedOnType(Class<?> returnType) {
		// if the underlying StringBridge accepts it, call the method
		if ( stringBridge instanceof AppliedOnTypeAwareBridge ) {
			( (AppliedOnTypeAwareBridge) stringBridge ).setAppliedOnType( returnType );
		}
	}
}
