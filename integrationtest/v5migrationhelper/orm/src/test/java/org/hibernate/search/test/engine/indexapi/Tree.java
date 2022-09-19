/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.indexapi;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Tree {
	@Id
	@GeneratedValue
	private long id;

	@Field
	private String species;

	@OneToMany(mappedBy = "tree", cascade = CascadeType.ALL)
	@IndexedEmbedded
	private Set<Leaf> leaves;

	Tree() {
	}

	public Tree(String species) {
		this.species = species;
		this.leaves = new HashSet<Leaf>();
	}

	public String getSpecies() {
		return species;
	}

	public Set<Leaf> getLeaves() {
		return leaves;
	}

	public void growNewLeave() {
		leaves.add( new Leaf() );
	}
}


