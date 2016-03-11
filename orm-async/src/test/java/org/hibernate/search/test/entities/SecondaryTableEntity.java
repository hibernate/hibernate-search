/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import javax.persistence.SecondaryTable;

/**
 * @author Martin Braun
 */
@Entity
@Table(name = "PRIMARY")
@SecondaryTables(@SecondaryTable(name = "SECONDARY",
		pkJoinColumns = @PrimaryKeyJoinColumn(name = "SEC_ID", referencedColumnName = "ID")
))
public class SecondaryTableEntity {

	@Id
	@Column(name = "ID")
	public Long id;

	@Column(name = "SEC_STRING", table = "SECONDARY")
	public String secondary;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSecondary() {
		return secondary;
	}

	public void setSecondary(String secondary) {
		this.secondary = secondary;
	}
}
