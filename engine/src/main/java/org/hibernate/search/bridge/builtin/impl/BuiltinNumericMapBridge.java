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

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * It manages an {@link java.util.Map} object annotated with {@link org.hibernate.search.annotations.NumericField}
 *
 * @author Davide D'Alto
 */
public class BuiltinNumericMapBridge extends BuiltinMapBridge {

	public BuiltinNumericMapBridge(FieldBridge fieldBridge) {
		super( fieldBridge );
	}

	public BuiltinNumericMapBridge() {
		super( new FieldBridge() {

			@Override
			public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
				if ( value == null ) {
					manageNull( name, document, luceneOptions );
				}
				else {
					luceneOptions.addNumericFieldToDocument( name, value, document );
				}
			}

			private void manageNull(String name, Document document, LuceneOptions luceneOptions) {
				if ( luceneOptions.indexNullAs() != null ) {
					luceneOptions.addFieldToDocument( name, luceneOptions.indexNullAs(), document );
				}
			}

		} );
	}

}
