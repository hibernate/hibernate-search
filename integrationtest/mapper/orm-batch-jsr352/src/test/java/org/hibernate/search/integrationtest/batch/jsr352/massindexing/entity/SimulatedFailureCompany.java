/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.integrationtest.batch.jsr352.util.SimulatedFailure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Access(value = AccessType.PROPERTY) // Necessary to hook the getters on EntityWriter phase
@Indexed
public class SimulatedFailureCompany {

	private int id;

	private String name;

	public SimulatedFailureCompany() {
		// Called by Hibernate ORM entity loading, which in turn
		// is called by the EntityReader phase of the batch.
		SimulatedFailure.read();
	}

	public SimulatedFailureCompany(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	public int getId() {
		// Called by Hibernate Search indexer#addOrUpdate, which in turn
		// is called by the EntityWriter phase of the batch.
		SimulatedFailure.write();

		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@FullTextField
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "SimulatedFailureCompany [id=" + id + ", name=" + name + "]";
	}

}
