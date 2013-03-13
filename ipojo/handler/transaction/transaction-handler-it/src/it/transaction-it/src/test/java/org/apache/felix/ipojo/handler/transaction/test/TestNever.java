/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.handler.transaction.test;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.handler.transaction.components.FooDelegator;
import org.apache.felix.ipojo.handler.transaction.components.FooImpl;
import org.apache.felix.ipojo.handler.transaction.services.CheckService;
import org.apache.felix.ipojo.handler.transaction.services.Foo;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.transaction.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy.withiPOJO;

public class TestNever extends Common {

    public static final File TEST = new File("src/main/resources");

    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();


        InputStream service = TinyBundles.bundle()
                .add(CheckService.class)
                .add(Foo.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Service")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build();

        InputStream fooimpl = TinyBundles.bundle()
                .add(FooImpl.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Foo Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build(withiPOJO(new File(TEST, "foo.xml")));

        InputStream test = TinyBundles.bundle()
                .add(FooDelegator.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "NeverTransactionPropagation")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services, javax.transaction;version=1.1")
                .build(withiPOJO(new File(TEST, "never.xml")));

        return OptionUtils.combine(
                options,
                provision(
                        service,
                        fooimpl,
                        test
                ));
    }

    @Test
    public void testOkOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("never-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingGood();
    }

    @Test(expected = RuntimeException.class)
    public void testOkInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("never-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        cs.doSomethingGood(); // Fail
    }

    @Test(expected = NullPointerException.class)
    public void testExceptionOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("never-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingBad(); // Throws an NPE
    }

    @Test
    public void testExceptionInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("never-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad(); // Fail
            Assert.fail("RuntimeException expected");
        } catch (RuntimeException e) {
            // Ok :-)
        }
        Assert.assertEquals(Status.STATUS_ACTIVE, t.getStatus());

        t.rollback();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExpectedExceptionOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("never-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingBad2();
    }


}
