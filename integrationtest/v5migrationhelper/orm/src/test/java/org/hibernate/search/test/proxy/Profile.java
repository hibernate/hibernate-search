/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.proxy;

import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Indexed
@Table(name = "profile")
@Proxy(proxyClass = IProfile.class)
public class Profile implements IProfile {

	private Integer id;
	private Set<IComment> comments;

	@Override
	@Id
	@DocumentId
	@Column(name = "profileid")
	public Integer getId() {
		return id;
	}
	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	@OneToMany(targetEntity = Comment.class, mappedBy = "profile")
	@IndexedEmbedded(targetElement = Comment.class)
	public Set<IComment> getComments() {
		return comments;
	}

	@Override
	public void setComments(Set<IComment> c) {
		this.comments = c;
	}

}
