/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Recipe {
	@Id
	@GeneratedValue
	private int id;

	@Field
	private String name;

	@ManyToMany
	@IndexedEmbedded
	private Set<Ingredient> ingredients = new HashSet<>();

	public Recipe() {
	}

	public Recipe(String name) {
		this.name = name;
	}

	public void addIngredients(Ingredient... ingredients) {
		Collections.addAll( this.ingredients, ingredients );
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Recipe{" +
				"id=" + id +
				", name='" + name + '\'' +
				", ingredients=" + ingredients +
				'}';
	}
}
