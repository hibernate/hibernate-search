/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;

@Entity
public class Leaf {

	@DocumentId
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany
	@Field(bridge = @FieldBridge(impl = CollectionItemFieldBridge.class))
	private List<CollectionItem> collectionItems = new ArrayList<CollectionItem>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<CollectionItem> getCollectionItems() {
		return collectionItems;
	}

	public void setBridgedEntities(List<CollectionItem> collectionItems) {
		this.collectionItems = collectionItems;
	}

}
