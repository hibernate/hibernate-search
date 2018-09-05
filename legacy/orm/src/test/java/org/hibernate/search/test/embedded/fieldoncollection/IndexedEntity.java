/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed
public class IndexedEntity {

	public static final String FIELD1_FIELD_NAME = "field1";
	public static final String FIELD2_FIELD_NAME = "field2";

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String name;

	@ManyToMany(targetEntity = CollectionItem.class)
	@JoinTable(name = "ent_collection_field")
	@Field(bridge = @FieldBridge(impl = CollectionItemFieldBridge.class), analyze = Analyze.NO)
	private List<CollectionItem> itemsWithFieldAnnotation = new ArrayList<CollectionItem>();

	@ManyToMany(targetEntity = CollectionItem.class)
	@JoinTable(name = "ent_collection_fields")
	@Fields({
			@Field(name = FIELD1_FIELD_NAME, bridge = @FieldBridge(impl = CollectionItemFieldBridge.class), analyze = Analyze.NO),
			@Field(name = FIELD2_FIELD_NAME, bridge = @FieldBridge(impl = CollectionItemFieldBridge.class), analyze = Analyze.NO)
	})
	private List<CollectionItem> itemsWithFieldsAnnotation = new ArrayList<CollectionItem>();

	@ElementCollection
	@Column(name = "keyword")
	@CollectionTable(name = "indexedentity_keyword", joinColumns = { @JoinColumn(name = "indexedentity") })
	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class), analyze = Analyze.NO, store = Store.YES)
	private Set<String> keywords = new HashSet<String>();

	public IndexedEntity() {
	}

	public IndexedEntity(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CollectionItem> getItemsWithFieldAnnotation() {
		return itemsWithFieldAnnotation;
	}

	public void setItemsWithFieldAnnotation(List<CollectionItem> items) {
		this.itemsWithFieldAnnotation.clear();

		for ( CollectionItem item : items ) {
			this.addItemsWithFieldAnnotation( item );
		}
	}

	public void addItemsWithFieldAnnotation(CollectionItem item) {
		if ( !this.itemsWithFieldAnnotation.contains( item ) ) {
			this.itemsWithFieldAnnotation.add( item );
		}
	}

	public List<CollectionItem> getItemsWithFieldsAnnotation() {
		return itemsWithFieldsAnnotation;
	}

	public void setItemsWithFieldsAnnotation(List<CollectionItem> items) {
		this.itemsWithFieldsAnnotation.clear();

		for ( CollectionItem item : items ) {
			this.addItemsWithFieldsAnnotation( item );
		}
	}

	public void addItemsWithFieldsAnnotation(CollectionItem item) {
		if ( !this.itemsWithFieldsAnnotation.contains( item ) ) {
			this.itemsWithFieldsAnnotation.add( item );
		}
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords.clear();

		for ( String keyword : keywords ) {
			this.addKeyword( keyword );
		}
	}

	public void addKeyword(String keyword) {
		this.keywords.add( keyword );
	}
}
