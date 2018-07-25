/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Borrowal extends AbstractEntity<Integer> {

	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne(optional = false)
	private Account account;

	@ManyToOne(optional = false)
	private DocumentCopy<?> copy;

	@Basic(optional = false)
	private BorrowalType type;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public DocumentCopy<?> getCopy() {
		return copy;
	}

	public void setCopy(DocumentCopy<?> copy) {
		this.copy = copy;
	}

	public BorrowalType getType() {
		return type;
	}

	public void setType(BorrowalType type) {
		this.type = type;
	}

	@Override
	protected String getDescriptionForToString() {
		return "account=" + getAccount() + ",document=" + getCopy();
	}
}
