/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.batchindexing;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Bayo Erinle
 */
public class LegacyTirePKBridge implements TwoWayFieldBridge {

	private static final String CAR_ID = ".carId";
	private static final String TIRE_ID = ".tireId";

	@Override
	public Object get(String name, Document document) {
		LegacyTirePK id = new LegacyTirePK();
		Fieldable field = document.getFieldable( name + CAR_ID );
		id.setCarId( field.stringValue() );
		field = document.getFieldable( name + TIRE_ID );
		id.setTireId( field.stringValue() );
		return id;
	}

	@Override
	public String objectToString(Object o) {
		LegacyTirePK id = (LegacyTirePK) o;
		return new StringBuilder().append( id.getCarId() ).append( "-" ).append( id.getTireId() ).toString();
	}

	@Override
	public void set(String name, Object o, Document document, LuceneOptions luceneOptions) {
		LegacyTirePK id = (LegacyTirePK) o;
		luceneOptions.addFieldToDocument( name + CAR_ID, id.getCarId(), document );
		luceneOptions.addFieldToDocument( name + TIRE_ID, id.getTireId(), document );
	}
}
