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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.sample.jakarta.jsonb;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Test class demonstrating invalid Jsonb.close() usage with concurrent threads.
 * This should trigger a diagnostic warning.
 */
public class JsonbCloseInvalid {

    /**
     * Invalid: close() is called without proper thread synchronization.
     * A thread is started but not joined before closing the Jsonb instance.
     */
    public void unsafeCloseWithThread() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        
        Thread thread = new Thread(() -> {
            // Thread might still be using jsonb
            String json = jsonb.toJson(new Object());
        });
        thread.start();
        
        // WARNING: close() called without waiting for thread to finish
        jsonb.close();
    }

    /**
     * Invalid: close() is called with executor service without proper shutdown.
     */
    public void unsafeCloseWithExecutor() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        
        executor.submit(() -> {
            String json = jsonb.toJson(new Object());
            return json;
        });
        
        // WARNING: close() called without executor shutdown/awaitTermination
        jsonb.close();
    }

    /**
     * Invalid: close() with multiple thread operations and no synchronization.
     */
    public void unsafeCloseWithMultipleThreads() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        
        Thread t1 = new Thread(() -> jsonb.toJson("data1"));
        Thread t2 = new Thread(() -> jsonb.toJson("data2"));
        
        t1.start();
        t2.start();
        
        // WARNING: close() called without joining threads
        jsonb.close();
    }

    /**
     * Invalid: Runnable with execute and no synchronization.
     */
    public void unsafeCloseWithRunnable() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        
        Runnable task = () -> {
            jsonb.toJson(new Object());
        };
        executor.execute(task);
        
        // WARNING: close() called without synchronization
        jsonb.close();
    }
}

// Made with Bob
