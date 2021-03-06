package com.hedera.hcsapp.controllers;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hedera.hcsapp.Statics;
import com.hedera.hcsapp.dockercomposereader.DockerCompose;
import com.hedera.hcsapp.dockercomposereader.DockerComposeReader;
import com.hedera.hcsapp.dockercomposereader.DockerService;
import com.hedera.hcsapp.entities.AddressBook;
import com.hedera.hcsapp.repository.AddressBookRepository;
import com.hedera.hcsapp.restclasses.AddressBookRest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
public class AddressBookController {

    public AddressBookController() throws Exception {
    }

    @Autowired
    AddressBookRepository addressBookRepository;

    @GetMapping(value = "/addressbook/buyerseller", produces = "application/json")
    public List<AddressBook> addressbookBuyerSeller() throws Exception {
        log.debug("/addressbook/buyerorseller");
        return addressBookRepository.findAllWithRoleButMe(Statics.getAppData().getUserName(),"BUYER,SELLER");
    }

    @GetMapping(value = "/addressbook/appusers", produces = "application/json")
    public ResponseEntity<List<AddressBookRest>> addressbookAppUsers() throws Exception {
        log.debug("/addressbook/appusers");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        List<AddressBookRest> restResponse = new ArrayList<AddressBookRest>();
        
        DockerCompose dockerCompose = DockerComposeReader.parse();
        
        for (Map.Entry<String, DockerService> dockerService : dockerCompose.getServices().entrySet()) {
            DockerService service = dockerService.getValue();
            if ((null != service.getEnvironment()) && (service.getEnvironment().containsKey("APP_ID"))) {
                
                String name = service.getContainer_name();
                String publicKey = service.getEnvironment().get("PUBKEY");
                String roles = service.getEnvironment().get("ROLES");
                String paymentAccountDetails = service.getEnvironment().get("PAYMENT_ACCOUNT_DETAILS");
                String color = service.getEnvironment().get("COLOR");
                long port = Long.parseLong(service.getPort());
                String appId = service.getEnvironment().get("APP_ID");
                
                restResponse.add(new AddressBookRest(name, publicKey, roles, paymentAccountDetails, port, color, appId));
            }
            
        }
        return new ResponseEntity<>(restResponse, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/addressbook/paychannel", produces = "application/json")
    public List<AddressBook> addressbookPaychannel() throws Exception {
        log.debug("/addressbook/paychannel");
        return addressBookRepository.findAllWithRoleButMe(Statics.getAppData().getUserName(),"PAYCHANNEL");
    }
    
    @GetMapping(value = "/addressbook-everything", produces = "application/json")
    public List<AddressBook> addressbookEverything() throws FileNotFoundException, IOException {
        return addressBookRepository.findAllUsers();
    }
    
    @GetMapping(value = "/addressbook", produces = "application/json")
    public List<AddressBook> addressbookAll() throws Exception {
        log.debug("/addressbook");
        return addressBookRepository.findAllUsersButMe(Statics.getAppData().getUserName());
    }
    @GetMapping(value = "/addressbook/me", produces = "application/json")
    public AddressBook addressbookMe() throws Exception {
        log.debug("/addressbook");
        return addressBookRepository.findById(Statics.getAppData().getUserName()).get();
    }
}
