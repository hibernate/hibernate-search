/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(interceptor = IndexWhenPublishedInterceptor.class)
public class Blog {
	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		return getClass().equals( obj.getClass() )
				&& id != null
				&& Objects.equals( id, ((Blog) obj).id );
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	private Integer id;

	@Field
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private String title;

	//state used by interceptor but not indexed on purpose to test optimization disablement
	public BlogStatus getStatus() {
		return status;
	}

	public void setStatus(BlogStatus status) {
		this.status = status;
	}

	private BlogStatus status;
}
