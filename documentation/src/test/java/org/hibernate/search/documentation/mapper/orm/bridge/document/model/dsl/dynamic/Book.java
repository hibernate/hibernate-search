/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.dynamic;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.SortNatural;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@ElementCollection
	@SortNatural
	@PropertyBinding(binder = @PropertyBinderRef(type = UserMetadataBinder.class))
	private Map<String, String> userMetadata = new TreeMap<>();

	@ElementCollection
	@SortNatural
	@PropertyBinding(binder = @PropertyBinderRef(type = MultiTypeUserMetadataBinder.class))
	private Map<String, Serializable> multiTypeUserMetadata = new TreeMap<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}

	public Map<String, Serializable> getMultiTypeUserMetadata() {
		return multiTypeUserMetadata;
	}
}
