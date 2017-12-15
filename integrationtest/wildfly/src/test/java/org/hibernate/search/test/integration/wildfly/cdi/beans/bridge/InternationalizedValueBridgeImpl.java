/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.beans.bridge;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.integration.wildfly.cdi.beans.event.BridgeCDILifecycleEventCounter;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.Language;
import org.hibernate.search.test.integration.wildfly.cdi.beans.i18n.LocalizationService;

import org.apache.lucene.document.Document;

/**
 * @author Yoann Rodiere
 */
/*
 * The @Dependent context is not really necessary for this implementation, since it is stateless.
 * However, we want to test that it is possible to create one instance per @Field annotation.
 * See CDIInjectionLifecycleEventsIT.
 */
@Dependent
public class InternationalizedValueBridgeImpl implements InternationalizedValueBridge {

	@Inject
	private LocalizationService localizationService;

	@Inject
	private BridgeCDILifecycleEventCounter counter;

	@PostConstruct
	public void construct() {
		counter.onFieldBridgeConstruct();
	}

	@PreDestroy
	public void destroy() {
		counter.onFieldBridgeDestroy();
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.STRING );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		for ( Language language : Language.values() ) {
			String localizedValue = localizationService.localize( (InternationalizedValue) value, language );
			luceneOptions.addFieldToDocument( name, localizedValue, document );
		}
	}

}