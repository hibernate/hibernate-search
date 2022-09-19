/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.annotation;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	private String title;

	@BooleanAsStringField(trueAsString = "yes", falseAsString = "no") // <1>
	private boolean published;

	@ElementCollection
	@BooleanAsStringField( // <2>
			name = "censorshipAssessments_allYears",
			trueAsString = "passed", falseAsString = "failed"
	)
	private Map<Year, Boolean> censorshipAssessments = new HashMap<>();

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Map<Year, Boolean> getCensorshipAssessments() {
		return censorshipAssessments;
	}

	public void setCensorshipAssessments(Map<Year, Boolean> censorshipAssessments) {
		this.censorshipAssessments = censorshipAssessments;
	}
// end::getters-setters[]
}
// end::include[]
