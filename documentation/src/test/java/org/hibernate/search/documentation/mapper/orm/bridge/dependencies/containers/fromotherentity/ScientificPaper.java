/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.dependencies.containers.fromotherentity;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

//tag::include[]
@Entity
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = ScientificPapersReferencedByBinder.class)) // <1>
public class ScientificPaper {

	@Id
	private Integer id;

	private String title;

	@ManyToMany
	private List<ScientificPaper> references = new ArrayList<>();

	public ScientificPaper() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public ScientificPaper(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ScientificPaper> getReferences() {
		return references;
	}
	//end::getters-setters[]
}
//end::include[]
