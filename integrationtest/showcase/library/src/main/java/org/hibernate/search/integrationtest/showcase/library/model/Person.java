/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.StringJoiner;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity
@Indexed(index = Person.INDEX)
public class Person extends AbstractEntity<Integer> {

	static final String INDEX = "Person";

	@Id
	private Integer id;

	@Basic(optional = false)
	// TODO use a different analyzer/normalizer for these fields
	@Field(analyzer = "default")
	@Field(name = "firstName_sort", sortable = Sortable.YES)
	private String firstName;

	@Basic(optional = false)
	// TODO use a different analyzer/normalizer for these fields
	@Field(analyzer = "default")
	@Field(name = "lastName_sort", sortable = Sortable.YES)
	private String lastName;

	@OneToOne(mappedBy = "user")
	@IndexedEmbedded
	private Account account;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	@Override
	protected String getDescriptionForToString() {
		return new StringJoiner( "," )
				.add( getLastName() )
				.add( getFirstName() )
				.toString();
	}
}
