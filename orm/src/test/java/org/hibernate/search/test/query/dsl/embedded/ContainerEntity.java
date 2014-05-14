/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl.embedded;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author neek
 */
@Entity
@Indexed
public class ContainerEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String parentStringValue;

	@Embedded
	@IndexedEmbedded(depth = 1, prefix = "emb.")
	private EmbeddedEntity embeddedEntity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getParentStringValue() {
		return parentStringValue;
	}

	public void setParentStringValue(String parentStringValue) {
		this.parentStringValue = parentStringValue;
	}

	public EmbeddedEntity getEmbeddedEntity() {
		return embeddedEntity;
	}

	public void setEmbeddedEntity(EmbeddedEntity embeddedEntity) {
		this.embeddedEntity = embeddedEntity;
	}

}
