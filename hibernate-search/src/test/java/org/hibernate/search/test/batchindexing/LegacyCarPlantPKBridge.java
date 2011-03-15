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
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Bayo Erinle
 */
public class LegacyCarPlantPKBridge implements TwoWayFieldBridge {
	private static final String PLANT_ID = ".plantId";
	private static final String CAR_ID = ".carId";

	public Object get(String name, Document document) {
		LegacyCarPlantPK id = new LegacyCarPlantPK();
		Field field = document.getField( name + PLANT_ID );
		id.setPlantId( field.stringValue() );

		field = document.getField( name + CAR_ID );
		id.setCarId( field.stringValue() );
		return id;
	}

	public String objectToString(Object o) {
		LegacyCarPlantPK id = (LegacyCarPlantPK) o;
		StringBuilder sb = new StringBuilder();
		sb.append( id.getPlantId() ).append( "-" ).append( id.getCarId() );
		return sb.toString();
	}

	public void set(String name, Object o, Document document, LuceneOptions luceneOptions) {
		LegacyCarPlantPK id = (LegacyCarPlantPK) o;
		Field.Store store = luceneOptions.getStore();
		Field.Index index = luceneOptions.getIndex();
		Field.TermVector termVector = luceneOptions.getTermVector();
		Float boost = luceneOptions.getBoost();

		Field field = new Field( name + PLANT_ID, id.getPlantId(), store, index, termVector );
		field.setBoost( boost );
		document.add( field );

		field = new Field( name + CAR_ID, id.getCarId(), store, index, termVector );
		field.setBoost( boost );
		document.add( field );
	}
}
