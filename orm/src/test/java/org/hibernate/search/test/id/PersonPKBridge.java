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
package org.hibernate.search.test.id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Emmanuel Bernard
 */
public class PersonPKBridge implements TwoWayFieldBridge {

	@Override
	public Object get(String name, Document document) {
		PersonPK id = new PersonPK();
		Fieldable field = document.getFieldable( name + ".firstName" );
		id.setFirstName( field.stringValue() );
		field = document.getFieldable( name + ".lastName" );
		id.setLastName( field.stringValue() );
		return id;
	}

	@Override
	public String objectToString(Object object) {
		PersonPK id = (PersonPK) object;
		StringBuilder sb = new StringBuilder();
		sb.append( id.getFirstName() ).append( " " ).append( id.getLastName() );
		return sb.toString();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		PersonPK id = (PersonPK) value;

		//store each property in a unique field
		luceneOptions.addFieldToDocument( name + ".firstName", id.getFirstName(), document );

		luceneOptions.addFieldToDocument( name + ".lastName", id.getLastName(), document );

		//store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( id ), document );

	}

}
