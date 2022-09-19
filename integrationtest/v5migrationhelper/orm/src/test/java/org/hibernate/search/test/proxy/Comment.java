/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.proxy;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.Field;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 * @author Tom Kuo
 */
@Entity
@Table(name = "comments")
@Proxy(proxyClass = IComment.class)
public class Comment implements IComment {

	private Integer id;
	private IProfile parent;
	private String name;
	private IComment root;
	private List<IComment> replies = new ArrayList<IComment>();

	@Override
	@Id
	@Column(name = "commentid")
	public Integer getId() {
		return id;
	}
	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	@ManyToOne(targetEntity = Profile.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "profileid")
	public IProfile getProfile() {
		if ( parent == null && getRootComment() != null ) {
			return getRootComment().getProfile();
		}
		return parent;
	}

	@Override
	public void setProfile(IProfile p) {
		this.parent = p;
	}

	@Override
	@Column(name = "content")
	@Field(name = "content")
	public String getContent() {
		return name;
	}
	@Override
	public void setContent(String name) {
		this.name = name;
	}

	@Override
	@ManyToOne(targetEntity = Comment.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "rootid")
	public IComment getRootComment() {
		return root;
	}
	@Override
	public void setRootComment(IComment root) {
		this.root = root;
	}

	@Override
	@OneToMany(targetEntity = Comment.class, mappedBy = "rootComment", fetch = FetchType.LAZY)
	@Cascade(CascadeType.DELETE)
	public List<IComment> getReplies() {
		return replies;
	}

	@Override
	public void setReplies(List<IComment> replies) {
		this.replies = replies;
	}
}
