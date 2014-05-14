/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.optimizations.mappedsuperclasscollection;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;

@MappedSuperclass
public class MappedSuperclassWithCollectionField {

	@Id
	@GeneratedValue
	private Long id;

	@ElementCollection
	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class), analyze = Analyze.NO)
	private Collection<String> collection = new ArrayList<String>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<String> getCollection() {
		return collection;
	}

}
