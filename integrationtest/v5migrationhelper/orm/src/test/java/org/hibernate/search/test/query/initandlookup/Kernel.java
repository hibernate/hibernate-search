/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.initandlookup;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.StaticIndexingSwitch;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Cacheable(true)
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed(
		routingBinder = @RoutingBinderRef(type = StaticIndexingSwitch.Binder.class)
)
public class Kernel {

	@Id
	@GeneratedValue
	@DocumentId
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	@Field
	public String getCodeName() { return codeName; }
	public void setCodeName(String codeName) { this.codeName = codeName; }
	private String codeName;

	@Field
	public String getProduct() { return product; }
	public void setProduct(String product) { this.product = product; }
	private String product;

}
