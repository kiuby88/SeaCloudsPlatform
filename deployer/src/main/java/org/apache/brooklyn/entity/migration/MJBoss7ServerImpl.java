package org.apache.brooklyn.entity.migration;


import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.entity.java.UsesJmx;
import org.apache.brooklyn.entity.migration.effectors.MigrateEffector;
import org.apache.brooklyn.entity.webapp.jboss.JBoss7ServerImpl;
import org.apache.brooklyn.feed.jmx.JmxHelper;
import org.apache.brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;

@Beta
public class MJBoss7ServerImpl extends JBoss7ServerImpl implements MJBoss7Server {

    private static final Logger LOG = LoggerFactory.getLogger(MJBoss7ServerImpl.class);

    public MJBoss7ServerImpl() {
        super();
    }

    @Override
    public void migrate(@EffectorParam(name = MigrateEffector.MIGRATE_LOCATION_SPEC, description = "Location Spec", nullable = false) String locationSpec) {

        if (sensors().get(Attributes.SERVICE_STATE_ACTUAL) != Lifecycle.RUNNING) {
            // Is it necessary to check if the whole application is healthy?
            throw new RuntimeException("The entity needs to be healthy before the migration starts");
        }

        if (getParent() != null && !getParent().equals(getApplication())) {
            /*
             * TODO: Allow nested entites to be migrated
             * If the entity has a parent different to the application root the migration cannot be done right now,
             * as it could lead into problems to deal with hierarchies like SameServerEntity -> Entity
             */
            throw new RuntimeException("Nested entities cannot be migrated right now");
        }

        // Retrieving the location from the catalog.
        Location newLocation = Entities.getManagementContext(this).getLocationRegistry().resolve(locationSpec);

        // TODO: Find a better way to check if you're migrating an entity to the exactly same VM. This not always works.
        for (Location oldLocation : getLocations()) {
            if (oldLocation.containsLocation(newLocation)) {
                LOG.warn("You cannot migrate an entity to the same location, the migration process will stop right now");
                return;
            }
        }

        LOG.info("Migration process of " + getId() + " started.");

        // When we have the new location, we free the resources of the current instance
        invoke(Startable.STOP, MutableMap.<String, Object>of()).blockUntilEnded();

        // Clearing old locations to remove the relationship with the previous instance
        clearLocations();
        addLocations(Lists.newArrayList(newLocation));

        // Starting the new instance

        invoke(Startable.START, MutableMap.<String, Object>of()).blockUntilEnded();
        String hostName = (String) getAttribute(Attributes.HOSTNAME);

        //entity.sensors().set(UsesJmx.JMX_URL, JmxHelper.toJmxmpUrl(entity.sensors(hostName, entity.getAttribute(UsesJmx.JMX_PORT)));

        sensors().set(UsesJmx.JMX_URL, JmxHelper.toJmxmpUrl(hostName, getAttribute(UsesJmx.JMX_PORT)));
        // Refresh all the dependent entities

        for (Entity applicationChild : getApplication().getChildren()) {
            // TODO: Find a better way to refresh the application configuration.
            // TODO: Refresh nested entities or find a way to propagate the restart properly.

            // Restart any entity but the migrated one.
            if (applicationChild instanceof Startable) {
                if (this.equals(applicationChild)) {
                    // The entity is sensors should rewired automatically on stop() + restart()
                } else {
                    // Restart the entity to fetch again all the dependencies (ie. attributeWhenReady ones)
                    ((Startable) applicationChild).restart();
                }
            }
        }

        LOG.info("Migration process of " + getId() + " finished.");
    }


}
