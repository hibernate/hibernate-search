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

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * It manages arrays annotated with {@link org.hibernate.search.annotations.NumericField}
 *
 * @author Davide D'Alto
 */
public class BuiltinNumericArrayBridge extends BuiltinArrayBridge {

	public BuiltinNumericArrayBridge(FieldBridge fieldBridge) {
		super( fieldBridge );
	}

	public BuiltinNumericArrayBridge() {
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
