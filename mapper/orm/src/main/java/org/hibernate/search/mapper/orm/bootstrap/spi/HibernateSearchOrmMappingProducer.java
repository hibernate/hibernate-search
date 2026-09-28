package org.hibernate.search.mapper.orm.bootstrap.spi;

import java.util.function.Consumer;

import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public interface HibernateSearchOrmMappingProducer {

	Consumer<AdditionalMappingContributions> produceMappingContributor(
			ConfigurationPropertySource propertySource,
			MetadataBuildingContext buildingContext
	);

}
