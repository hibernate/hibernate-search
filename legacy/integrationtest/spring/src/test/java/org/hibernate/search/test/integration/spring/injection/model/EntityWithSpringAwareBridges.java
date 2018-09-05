/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.integration.spring.injection.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.spring.injection.search.InternationalizedValueBridge;
import org.hibernate.search.test.integration.spring.injection.search.InternationalizedValueClassBridge;
import org.hibernate.search.test.integration.spring.injection.search.NonSpringBridge;

/**
 * @author Yoann Rodiere
 */
@Entity
@Indexed
@ClassBridge(name = EntityWithSpringAwareBridges.CLASS_BRIDGE_FIELD_NAME, impl = InternationalizedValueClassBridge.class)
public class EntityWithSpringAwareBridges {

	public static final String CLASS_BRIDGE_FIELD_NAME = "classBridge.internationalizedValue";
	public static final String NON_SPRING_BRIDGE_FIELD_NAME = "nonSpringBridgeField";

	@Id
	@GeneratedValue
	private Long id;

	@Field(bridge = @FieldBridge(impl = InternationalizedValueBridge.class))
	@Field(name = NON_SPRING_BRIDGE_FIELD_NAME, bridge = @FieldBridge(impl = NonSpringBridge.class), analyze = Analyze.NO)
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
