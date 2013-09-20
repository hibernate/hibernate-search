/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query.dsl;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Adds a custom field to be queried via explicitly passed field bridge.
 *
 * @author Gunnar Morling
 */
public class MonthClassBridge implements FieldBridge {

	public static final String FIELD_NAME_1 = "monthValueAsRomanNumberFromClassBridge1";
	public static final String FIELD_NAME_2 = "monthValueAsRomanNumberFromClassBridge2";

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Month month = (Month) value;

		luceneOptions.addFieldToDocument(
				FIELD_NAME_1,
				new RomanNumberFieldBridge().objectToString( month.getMonthValue() ),
				document
		);

		luceneOptions.addFieldToDocument(
				FIELD_NAME_2,
				new RomanNumberFieldBridge().objectToString( month.getMonthValue() ),
				document
		);
	}
}
