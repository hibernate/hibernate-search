/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.proxy;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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
	@ContainedIn
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
