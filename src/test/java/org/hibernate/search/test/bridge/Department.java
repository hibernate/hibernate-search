// $Id$
package org.hibernate.search.test.bridge;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;

/**
 * @author John Griffin
 */
@Entity
@Indexed
@ClassBridge(name="branchnetwork",
			 index=Index.TOKENIZED,
			 store=Store.YES,
			 impl = CatFieldsClassBridge.class,
			 params = @Parameter( name="sepChar", value=" " ) )
public class Department {
	private int id;
	private String network;
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

	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getBranchHead() {
		return branchHead;
	}

	public void setBranchHead(String branchHead) {
		this.branchHead = branchHead;
	}

	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	@Field(index=Index.UN_TOKENIZED, store=Store.YES)
	public Integer getMaxEmployees() {
		return maxEmployees;
	}

	public void setMaxEmployees(Integer maxEmployees) {
		this.maxEmployees = maxEmployees;
	}
}