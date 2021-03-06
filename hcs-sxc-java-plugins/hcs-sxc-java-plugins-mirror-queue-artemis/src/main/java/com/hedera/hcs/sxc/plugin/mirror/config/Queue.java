package com.hedera.hcs.sxc.plugin.mirror.config;

/*-
 * ‌
 * hcs-sxc-java
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

public final class Queue {
    private String initialContextFactory = "";
    private String tcpConnectionFactory = "";
    
    public String getInitialContextFactory() {
        return this.initialContextFactory;
    }
    public void setInitialContextFactory(String contextFactory) {
        this.initialContextFactory = contextFactory;
    }

    public String getTcpConnectionFactory() {
        return this.tcpConnectionFactory;
    }
    public void setTcpConnectionFactory(String tcpConnectionFactory) {
        this.tcpConnectionFactory = tcpConnectionFactory;
    }
      
    
}
