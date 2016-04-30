package org.apache.brooklyn.entity.migration;

import org.apache.brooklyn.entity.java.UsesJmx;
import org.apache.brooklyn.entity.webapp.jboss.JBoss7ServerImpl;
import org.apache.brooklyn.entity.webapp.jboss.JBoss7SshDriver;
import org.apache.brooklyn.feed.jmx.JmxHelper;
import org.apache.brooklyn.location.ssh.SshMachineLocation;

import com.google.common.annotations.Beta;

@Beta
public class MJBoss7SshDriver extends JBoss7SshDriver {

    public MJBoss7SshDriver(JBoss7ServerImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void postLaunch() {
        super.postLaunch();
        entity.sensors().set(UsesJmx.JMX_URL, JmxHelper.toJmxmpUrl(getHostname(), entity.getAttribute(UsesJmx.JMX_PORT)));
    }

}
