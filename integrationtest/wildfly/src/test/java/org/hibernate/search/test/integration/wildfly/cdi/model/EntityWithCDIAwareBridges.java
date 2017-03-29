/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.integration.wildfly.cdi.bridge.InternationalizedValueBridge;
import org.hibernate.search.test.integration.wildfly.cdi.bridge.InternationalizedValueClassBridge;
import org.hibernate.search.test.integration.wildfly.cdi.i18n.InternationalizedValue;

/**
 * @author Yoann Rodiere
 */
@Entity
@Indexed
@ClassBridge(name = EntityWithCDIAwareBridges.CLASS_BRIDGE_FIELD_NAME, impl = InternationalizedValueClassBridge.class)
public class EntityWithCDIAwareBridges {

	public static final String CLASS_BRIDGE_FIELD_NAME = "classBridge.internationalizedValue";

	@Id
	@GeneratedValue
	private Long id;

	@Field(bridge = @FieldBridge(impl = InternationalizedValueBridge.class))
	private InternationalizedValue internationalizedValue;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public InternationalizedValue getInternationalizedValue() {
		return internationalizedValue;
	}

	public void setInternationalizedValue(InternationalizedValue internationalizedValue) {
		this.internationalizedValue = internationalizedValue;
	}

}
