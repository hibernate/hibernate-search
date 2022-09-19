/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
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

	@ElementCollection
	@Column(name = "keyword")
	@CollectionTable(name = "indexedentity_keyword", joinColumns = { @JoinColumn(name = "indexedentity") })
	@Field(analyze = Analyze.NO, store = Store.YES)
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
