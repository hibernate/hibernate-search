/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = SimpleEntity.NAME)
@Indexed(index = SimpleEntity.NAME)
public class SimpleEntity {

	public static final String NAME = "SimpleEntity";

	@Id
	private Integer id;

	private String name;

	public SimpleEntity() {
	}

	public SimpleEntity(Integer id) {
		this.id = id;
		this.name = "name-" + id;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SimpleEntity that = (SimpleEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name );
	}

	@Override
	public String toString() {
		return "SimpleEntity{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}
