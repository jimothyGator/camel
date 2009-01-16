/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.cxf;

import javax.xml.ws.WebServiceProvider;

import org.apache.camel.Processor;
import org.apache.camel.component.cxf.feature.MessageDataFormatFeature;
import org.apache.camel.component.cxf.feature.PayLoadDataFormatFeature;
import org.apache.camel.component.cxf.spring.CxfEndpointBean;
import org.apache.camel.component.cxf.util.CxfEndpointUtils;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;

/**
 * A consumer of exchanges for a service in CXF
 *
 * @version $Revision$
 */
public class CxfConsumer extends DefaultConsumer {
    private CxfEndpoint endpoint;
    private Server server;

    public CxfConsumer(CxfEndpoint endpoint, Processor processor) throws Exception {

        super(endpoint, processor);
        Bus bus = null;
        this.endpoint = endpoint;
        boolean isWebServiceProvider = false;
        if (endpoint.getApplicationContext() != null) {            
            bus = endpoint.getCxfEndpointBean().getBus();
            if (CxfEndpointUtils.getSetDefaultBus(endpoint)) {
                BusFactory.setThreadDefaultBus(bus);
            }
        } else {
            // now we just use the default bus here
            bus = BusFactory.getThreadDefaultBus();
        }
        
        Class serviceClass = CxfEndpointUtils.getServiceClass(endpoint);
        ServerFactoryBean svrBean = CxfEndpointUtils.getServerFactoryBean(serviceClass);
        isWebServiceProvider = CxfEndpointUtils.hasAnnotation(serviceClass,
                                                              WebServiceProvider.class);
        
        if (endpoint.isSpringContextEndpoint()) {
            endpoint.configure(svrBean);
        } else { // setup the serverFactoryBean with the URI parameters
            svrBean.setAddress(endpoint.getAddress());                       
            if (endpoint.getWsdlURL() != null) {
                svrBean.setWsdlURL(endpoint.getWsdlURL());
            }
        }
        
        svrBean.setServiceClass(serviceClass);
        
        if (CxfEndpointUtils.getServiceName(endpoint) != null) {
            svrBean.setServiceName(CxfEndpointUtils.getServiceName(endpoint));
        }
        if (CxfEndpointUtils.getServiceName(endpoint) != null) {
            svrBean.setEndpointName(CxfEndpointUtils.getPortName(endpoint));
        }
        
        DataFormat dataFormat = CxfEndpointUtils.getDataFormat(endpoint);

        svrBean.setInvoker(new CamelInvoker(this));

        // apply feature here
        if (!dataFormat.equals(DataFormat.POJO) && !isWebServiceProvider) {

            if (dataFormat.equals(DataFormat.PAYLOAD)) {
                svrBean.getFeatures().add(new PayLoadDataFormatFeature());
                // adding the logging feature here for debug
                //features.add(new LoggingFeature());
            } else if (dataFormat.equals(DataFormat.MESSAGE)) {
                svrBean.getFeatures().add(new MessageDataFormatFeature());
                //features.add(new LoggingFeature());
            }
        }
        svrBean.setBus(bus);
        svrBean.setStart(false);
        server = svrBean.create();

    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        server.start();
    }

    @Override
    protected void doStop() throws Exception {
        server.stop();
        super.doStop();
    }

    public CxfEndpoint getEndpoint() {
        return endpoint;
    }

}
