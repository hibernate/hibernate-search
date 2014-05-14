/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Bayo Erinle
 */
public class LegacyCarPlantPKBridge implements TwoWayFieldBridge {
	private static final String PLANT_ID = ".plantId";
	private static final String CAR_ID = ".carId";

	@Override
	public Object get(String name, Document document) {
		LegacyCarPlantPK id = new LegacyCarPlantPK();
		IndexableField field = document.getField( name + PLANT_ID );
		id.setPlantId( field.stringValue() );

		field = document.getField( name + CAR_ID );
		id.setCarId( field.stringValue() );
		return id;
	}

	@Override
	public String objectToString(Object o) {
		LegacyCarPlantPK id = (LegacyCarPlantPK) o;
		StringBuilder sb = new StringBuilder();
		sb.append( id.getPlantId() ).append( "-" ).append( id.getCarId() );
		return sb.toString();
	}

	@Override
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
