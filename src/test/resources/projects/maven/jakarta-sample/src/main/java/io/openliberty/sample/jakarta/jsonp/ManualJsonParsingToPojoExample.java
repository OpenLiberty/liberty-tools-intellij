/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.sample.jakarta.jsonp;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.StringReader;

public class ManualJsonParsingToPojoExample {

    public static class User {
        private String name;
        private int age;

        public User() {}

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    // Example 1: Manual parsing with Json.createReader followed by manual mapping
    public User parseUserManually(String jsonString) {
        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonObject json = reader.readObject();
        
        User user = new User();
        user.setName(json.getString("name"));
        user.setAge(json.getInt("age"));
        
        return user;
    }

    // Example 2: Another manual parsing pattern
    public User anotherManualParsing(String jsonString) {
        JsonObject json = Json.createReader(new StringReader(jsonString)).readObject();
        
        User user = new User();
        user.setName(json.getString("name"));
        user.setAge(json.getInt("age"));
        
        return user;
    }

    // Example 3: Multiple field mappings
    public User complexManualParsing(String jsonString) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();
        
        User user = new User();
        user.setName(jsonObject.getString("name"));
        user.setAge(jsonObject.getInt("age"));
        
        return user;
    }

    // Valid: Using JSON-B for direct POJO deserialization - no diagnostic expected
    public User parseUserWithJsonb(String jsonString) {
        Jsonb jsonb = JsonbBuilder.create();
        return jsonb.fromJson(jsonString, User.class);
    }
}
