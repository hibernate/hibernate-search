/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.update;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class SimpleParentEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String name;

	@OneToOne
	private SimpleChildEntity child;

	protected SimpleParentEntity() {
	}

	public SimpleParentEntity(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SimpleChildEntity getChild() {
		return child;
	}

	public void setChild(SimpleChildEntity child) {
		this.child = child;
	}

}
