/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.includepathsanddepth;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

// tag::include[]
@Entity
@Indexed
public class Human {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name")
	private String name;

	@FullTextField(analyzer = "name")
	private String nickname;

	@ManyToMany
	@IndexedEmbedded(includeDepth = 2, includePaths = { "parents.parents.name" })
	private List<Human> parents = new ArrayList<>();

	@ManyToMany(mappedBy = "parents")
	private List<Human> children = new ArrayList<>();

	public Human() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public List<Human> getParents() {
		return parents;
	}

	public void setParents(List<Human> parents) {
		this.parents = parents;
	}

	public List<Human> getChildren() {
		return children;
	}

	public void setChildren(List<Human> children) {
		this.children = children;
	}
	// end::getters-setters[]
}
// end::include[]
