/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.coordination.outboxpolling.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// See AuthorService#triggerValidationFailure
@Entity(name = "Author")
@Indexed
public class AuthorWithSlightlyDifferentIndex {

	@Id
	@GeneratedValue
	private Integer id;

	@FullTextField(analyzer = AnalyzerNames.DEFAULT)
	private String name;

	public AuthorWithSlightlyDifferentIndex() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
