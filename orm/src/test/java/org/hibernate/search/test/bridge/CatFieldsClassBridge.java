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
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;

/**
 * @author John Griffin
 */
public class CatFieldsClassBridge implements FieldBridge, ParameterizedBridge {

	private String sepChar;

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		this.sepChar = parameters.get( "sepChar" );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// In this particular class the name of the new field was passed
		// from the name field of the ClassBridge Annotation. This is not
		// a requirement. It just works that way in this instance. The
		// actual name could be supplied by hard coding it below.
		Department dep = (Department) value;
		String fieldValue1 = dep.getBranch();
		if ( fieldValue1 == null ) {
			fieldValue1 = "";
		}
		String fieldValue2 = dep.getNetwork();
		if ( fieldValue2 == null ) {
			fieldValue2 = "";
		}
		String indexedString = fieldValue1 + sepChar + fieldValue2;
		luceneOptions.addFieldToDocument( name, indexedString, document );
	}
}
