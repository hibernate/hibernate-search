/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.bridge;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;

/**
 * This is just a simple copy of the Department entity to allow
 * separation of the tests for ClassBridge and ClassBridges.
 *
 * @author John Griffin
 */
@Entity
@Indexed
@ClassBridges({
		@ClassBridge(name = "branchnetwork",
				store = Store.YES,
				impl = CatDeptsFieldsClassBridge.class,
				params = @Parameter(name = "sepChar", value = " ")),
		@ClassBridge(name = "equiptype",
				store = Store.YES,
				impl = EquipmentType.class,
				params = {
						@Parameter(name = "C", value = "Cisco"),
						@Parameter(name = "D", value = "D-Link"),
						@Parameter(name = "K", value = "Kingston"),
						@Parameter(name = "3", value = "3Com")
				})
})
public class Departments {
	private int id;
	private String network;
	private String manufacturer;
	private String branchHead;
	private String branch;
	private Integer maxEmployees;

	@Id
	@GeneratedValue
	@DocumentId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Field(store = Store.YES)
	public String getBranchHead() {
		return branchHead;
	}

	public void setBranchHead(String branchHead) {
		this.branchHead = branchHead;
	}

	@Field(store = Store.YES)
	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	@Field(store = Store.YES)
	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	public Integer getMaxEmployees() {
		return maxEmployees;
	}

	public void setMaxEmployees(Integer maxEmployees) {
		this.maxEmployees = maxEmployees;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
}
