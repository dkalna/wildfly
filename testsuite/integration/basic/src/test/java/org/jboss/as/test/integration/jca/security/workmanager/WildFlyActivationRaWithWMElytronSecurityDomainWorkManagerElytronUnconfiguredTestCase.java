/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.security.workmanager;

import java.util.function.Consumer;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.jca.security.WildFlyActivationRaWithElytronAuthContextTestCase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;

/**
 * Test security inflow with Jakarta Connectors work manager where RA is configured with Elytron security domain
 * and Workmanager is configured with Elytron security explicitly configured (it doesn't have elytron-enabled=true).
 * Default workmanager behavior if unconfigured is elytron is enabled so this should deploy successfully.
 */
@RunWith(Arquillian.class)
@ServerSetup({
        WildFlyActivationRaWithWMElytronSecurityDomainWorkManagerElytronUnconfiguredTestCase.ElytronSetup.class,
        WildFlyActivationRaWithWMElytronSecurityDomainWorkManagerElytronUnconfiguredTestCase.JcaSetup.class,
        WildFlyActivationRaWithWMElytronSecurityDomainWorkManagerElytronUnconfiguredTestCase.RaSetup.class})
@RunAsClient
public class WildFlyActivationRaWithWMElytronSecurityDomainWorkManagerElytronUnconfiguredTestCase {
    private static final String ADMIN_OBJ_JNDI_NAME = "java:jboss/admObj";
    private static final String WM_ELYTRON_SECURITY_DOMAIN_NAME = "RaRealmElytron";
    private static final String BOOTSTRAP_CTX_NAME = "wrongContext";

    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final PropertyFileBasedDomain domain = PropertyFileBasedDomain.builder()
                    .withName(WM_ELYTRON_SECURITY_DOMAIN_NAME)
                    .withUser("rauser", "rauserpassword")
                    .build();

            return new ConfigurableElement[]{domain};
        }
    }

    static class JcaSetup extends AbstractJcaSetup {
        private static final String WM_NAME = "wrongWM";

        @Override
        protected String getWorkManagerName() {
            return WM_NAME;
        }

        @Override
        protected String getBootstrapContextName() {
            return BOOTSTRAP_CTX_NAME;
        }
    }

    static class RaSetup extends AbstractRaSetup {
        private static final String RA_NAME = "wf-ra-wm-security-domain";

        @Override
        protected String getResourceAdapterName() {
            return RA_NAME;
        }

        @Override
        protected String getBootstrapContextName() {
            return BOOTSTRAP_CTX_NAME;
        }

        @Override
        protected String getAdminObjectJNDIName() {
            return ADMIN_OBJ_JNDI_NAME;
        }

        @Override
        protected Consumer<ModelNode> getAddRAOperationConsumer() {
            return addRaOperation -> {
                addRaOperation.get("wm-security").set(true);
                addRaOperation.get("wm-elytron-security-domain").set(WM_ELYTRON_SECURITY_DOMAIN_NAME);
                addRaOperation.get("wm-security-default-principal").set("wm-default-principal");
                addRaOperation.get("wm-security-default-groups").set(new ModelNode().setEmptyList().add("wm-default-group"));
            };
        }
    }

    @Deployment(name = "wf-ra-wm-security-domain-rar", testable = false, managed = false)
    public static Archive<?> rarDeployment() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addPackage(MultipleConnectionFactory1.class.getPackage());
        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "wf-ra-wm-security-domain-rar.rar").addAsLibrary(jar)
                .addAsManifestResource(WildFlyActivationRaWithElytronAuthContextTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return rar;
    }

    @ArquillianResource
    private Deployer deployer;

    @Test
    public void testUnconfiguredElytron() throws Throwable {
        deployer.deploy("wf-ra-wm-security-domain-rar");
        try {
            deployer.undeploy("wf-ra-wm-security-domain-rar");
        } catch (Exception ex) {
            // ignore
        }
    }
}
