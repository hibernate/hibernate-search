/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
@ClassBridge(impl = Territory.NameFieldBridge.class)
public class Territory {

	@Id
	private int id;

	private String name;

	Territory() {
	}

	public Territory(int id) {
		this.id = id;
	}

	public Territory(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static class NameFieldBridge implements MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			Territory territory = (Territory) value;

			String territoryName = territory.getName();
			luceneOptions.addFieldToDocument( "territoryName", territoryName, document );
			document.add( new SortedDocValuesField( "territoryName", new BytesRef( territoryName ) ) );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder
				.field( "territoryName", FieldType.STRING )
					.sortable( true );
		}
	}
}
