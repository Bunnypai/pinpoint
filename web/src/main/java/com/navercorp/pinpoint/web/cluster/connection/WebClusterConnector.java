/*
 *
 *  * Copyright 2014 NAVER Corp.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.navercorp.pinpoint.web.cluster.connection;

import com.navercorp.pinpoint.rpc.LoggingStateChangeEventListener;
import com.navercorp.pinpoint.rpc.PinpointSocket;
import com.navercorp.pinpoint.rpc.UnsupportOperationMessageListener;
import com.navercorp.pinpoint.rpc.client.PinpointClientFactory;
import com.navercorp.pinpoint.rpc.cluster.ClusterOption;
import com.navercorp.pinpoint.rpc.cluster.Role;
import com.navercorp.pinpoint.rpc.util.ClassUtils;
import com.navercorp.pinpoint.rpc.util.ClientFactoryUtils;
import com.navercorp.pinpoint.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Taejin Koo
 */
public class WebClusterConnector implements WebClusterConnectionProvider {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PinpointClientFactory clientFactory = new PinpointClientFactory();
    private final List<PinpointSocket> clusterSocketList = new ArrayList<PinpointSocket>();

    private final String connectString;

    public WebClusterConnector(String connectString) {
        this.connectString = connectString;
    }

    @Override
    public void start() {
        logger.info("{} initialization started.", ClassUtils.simpleClassName(this));

        clientFactory.setTimeoutMillis(1000 * 5);
        clientFactory.setMessageListener(UnsupportOperationMessageListener.getInstance());
        clientFactory.addStateChangeEventListener(LoggingStateChangeEventListener.INSTANCE);
        clientFactory.setProperties(Collections.EMPTY_MAP);

        ClusterOption clusterOption = new ClusterOption(true, WebUtils.getServerIdentifier(), Role.CALLER);
        clientFactory.setClusterOption(clusterOption);

        List<InetSocketAddress> connectHostList = parseConnectString(connectString);
        for (InetSocketAddress host : connectHostList) {
            PinpointSocket pinpointSocket = ClientFactoryUtils.createPinpointClient(host, clientFactory);
            clusterSocketList.add(pinpointSocket);
        }

        logger.info("{} initialization completed.", ClassUtils.simpleClassName(this));
    }

    @Override
    public void stop() {
        logger.info("{} destroying started.", ClassUtils.simpleClassName(this));

        if (clientFactory != null) {
            clientFactory.release();
        }

        logger.info("{} destroying completed.", ClassUtils.simpleClassName(this));
    }


    private List<InetSocketAddress> parseConnectString(String connectString) {
        List<InetSocketAddress> serverAddressList = new ArrayList<InetSocketAddress>();

        String[] hostsList = connectString.split(",");
        for (String host : hostsList) {
            int portIndex = host.lastIndexOf(":");
            if (portIndex >= 0 && portIndex < host.length() - 1) {
                String ip = host.substring(0, portIndex);
                int port = Integer.parseInt(host.substring(portIndex + 1));

                serverAddressList.add(new InetSocketAddress(ip, port));
            } else {
                logger.warn("Invalid address format({}, expected: 'ip:port')", host);
            }
        }

        return serverAddressList;
    }

    @Override
    public List<PinpointSocket> getClusterSocketList() {
        return clusterSocketList;
    }

}