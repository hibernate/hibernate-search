/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
public class Nation {

	private Integer id;
	private String name;
	private String code;
	private Set<Book> librariesHave = new HashSet<Book>();

	public Nation() {
	}

	public Nation(String name, String code) {
		this.name = name;
		this.code = code;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Field(analyze = Analyze.NO)
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@IndexedEmbedded
	@OneToMany(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	public Set<Book> getLibrariesHave() {
		return librariesHave;
	}

	public void setLibrariesHave(Set<Book> librariesHave) {
		this.librariesHave = librariesHave;
	}

}
