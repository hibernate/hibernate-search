/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.beans.bridge;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.integration.wildfly.cdi.beans.event.BridgeCDILifecycleEventCounter;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.Language;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.LocalizationService;
import org.hibernate.search.test.integration.wildfly.cdi.beans.model.EntityWithCDIAwareBridges;

import org.apache.lucene.document.Document;

/**
 * @author Yoann Rodiere
 */
@Singleton
public class InternationalizedValueClassBridgeImpl implements InternationalizedValueClassBridge {

	@Inject
	private LocalizationService localizationService;

	@Inject
	private BridgeCDILifecycleEventCounter counter;

	@PostConstruct
	public void construct() {
		counter.onClassBridgeConstruct();
	}

	@PreDestroy
	public void destroy() {
		counter.onClassBridgeDestroy();
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.STRING );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		EntityWithCDIAwareBridges entity = (EntityWithCDIAwareBridges) value;
		InternationalizedValue internationalizedValue = entity.getInternationalizedValue();
		for ( Language language : Language.values() ) {
			String localizedValue = localizationService.localize( internationalizedValue, language );
			luceneOptions.addFieldToDocument( name, localizedValue, document );
		}
	}

}