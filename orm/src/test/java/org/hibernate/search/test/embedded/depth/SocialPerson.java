/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.embedded.depth;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
@Indexed
public class SocialPerson {

	public SocialPerson() {
	}

	public SocialPerson(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long id;

	public String name;

	public Set<SocialPerson> friends = new HashSet<SocialPerson>();

	public Set<SocialPerson> friendsReverse = new HashSet<SocialPerson>();

	public void addFriends(SocialPerson... persons) {
		for ( SocialPerson p : persons ) {
			friends.add( p );
			p.friendsReverse.add( this );
		}
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(analyze = Analyze.NO)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@IndexedEmbedded(includePaths = { "name" })
	@ManyToMany
	public Set<SocialPerson> getFriends() {
		return friends;
	}
	
	public void setFriends(Set<SocialPerson> friends) {
		this.friends = friends;
	}

	@ManyToMany(mappedBy = "friends")
	@ContainedIn
	public Set<SocialPerson> getFriendsReverse() {
		return friendsReverse;
	}
	
	public void setFriendsReverse(Set<SocialPerson> friendsReverse) {
		this.friendsReverse = friendsReverse;
	}

}
