/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Gunnar Morling
 */
public class NameConcatenationBridge implements TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( name, objectToString( value ), document );
	}

	@Override
	public Object get(String name, Document document) {
		return document.get( name );
	}

	@Override
	public String objectToString(Object object) {
		GolfPlayer player = (GolfPlayer) object;
		StringBuilder names = new StringBuilder();
		if ( player.getFirstName() != null ) {
			names.append( player.getFirstName() ).append( " " );
		}
		if ( player.getLastName() != null ) {
			names.append( player.getLastName() );
		}
		return names.toString();
	}
}
