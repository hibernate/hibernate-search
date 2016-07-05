/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;

@Entity
@Table(name = "STUDENT")
@Indexed
public class StudentEntity {

	@EmbeddedId
	@DocumentId
	@FieldBridge(impl = StudentFieldBridge.class)
	private RegistrationId regid;

	@Field
	private String name;

	public RegistrationId getRegid() {
		return regid;
	}

	public void setRegid(RegistrationId regid) {
		this.regid = regid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
