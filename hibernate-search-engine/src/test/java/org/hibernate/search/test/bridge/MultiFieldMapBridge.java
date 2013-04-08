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
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
public class MultiFieldMapBridge implements FieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( ! ( value instanceof Map ) ) {
			throw new IllegalArgumentException( "This field can only be applied on a Map type field" );
		}
		else {
			Map<Object,Object> userValue = (Map) value;
			for ( Map.Entry<Object,Object> e : userValue.entrySet() ) {
				setField( name, String.valueOf( e.getKey() ), String.valueOf( e.getValue() ), document, luceneOptions );
			}
		}
	}

	private void setField(String fieldPrefix, String key, String value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( fieldPrefix + "." + key, value, document );
	}

}
