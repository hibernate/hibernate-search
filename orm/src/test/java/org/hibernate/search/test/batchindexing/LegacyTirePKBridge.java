/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

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
		IndexableField field = document.getField( name + CAR_ID );
		id.setCarId( field.stringValue() );
		field = document.getField( name + TIRE_ID );
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
