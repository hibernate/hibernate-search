/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * "Who Am I" is a poorly-formed entity. It has multiple ID-like fields, which make it difficult to identify the real
 * identifier.
 *
 * @author Mincong Huang
 */
@Entity
@Indexed
public class WhoAmI {

	@Id
	@DocumentId
	private String customId;

	@Field
	private String id;

	@Field
	private String uid;

	public WhoAmI() {
	}

	public WhoAmI(String customId, String id, String uid) {
		this.customId = customId;
		this.id = id;
		this.uid = uid;
	}

	public String getCustomId() {
		return customId;
	}

	public void setCustomId(String customId) {
		this.customId = customId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
}
