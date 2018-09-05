/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
