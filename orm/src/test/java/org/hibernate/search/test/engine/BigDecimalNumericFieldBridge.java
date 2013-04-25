/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
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
package org.hibernate.search.test.engine;

import java.math.BigDecimal;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;

public class BigDecimalNumericFieldBridge extends NumericFieldBridge {

	private static final BigDecimal storeFactor = BigDecimal.valueOf( 100 );

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			BigDecimal decimalValue = (BigDecimal) value;
			Long indexedValue = Long.valueOf( decimalValue.multiply( storeFactor ).longValue() );
			luceneOptions.addNumericFieldToDocument( name, indexedValue, document );
		}
	}

	@Override
	public Object get(String name, Document document) {
		String fromLucene = document.get( name );
		BigDecimal storedBigDecimal = new BigDecimal( fromLucene );
		return storedBigDecimal.divide( storeFactor );
	}

}
