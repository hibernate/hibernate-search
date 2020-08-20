/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.annotations.SortableField;
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
	@Field(name = "idSort")
	@SortableField(forField = "idSort")
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
