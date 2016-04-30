package org.apache.brooklyn.entity.migration;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.core.annotation.Effector;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.entity.migration.effectors.MigrateEffector;
import org.apache.brooklyn.entity.webapp.jboss.JBoss7Server;

import com.google.common.annotations.Beta;

@Beta
@Catalog(name = "Migrable Tomcat Server",
        iconUrl = "classpath:///tomcat-logo.png")
@ImplementedBy(MJBoss7ServerImpl.class)
public interface MJBoss7Server extends JBoss7Server, Migrable {

    /**
     * Starts a migration process.
     * It calls stop() on the original locations and start() on the new one.
     * <p/>
     * After this process finishes it refreshes all the sibling entities dependent data (ConfigKeys, Env variables...)
     */
    @Beta
    @Effector(description = "Migrates the current entity to another location. It will free the provisioned resources" +
            " used by the former location")
    void migrate(@EffectorParam(name = MigrateEffector.MIGRATE_LOCATION_SPEC, description = "Location Spec", nullable = false) String locationSpec);


}
