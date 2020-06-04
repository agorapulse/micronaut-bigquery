/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.bigquery.aws;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.util.Base64;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.SecurityUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.env.Environment;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

@ConfigurationProperties("bigquery.credentials")
public class BigQueryConfig {

    private final String decryptedPrivateKey;
    private String projectId;
    private String privateKeyId;
    private String clientEmail;
    private String clientId;

    public BigQueryConfig(Environment environment) {
        this.decryptedPrivateKey = decryptKMSIfRequired(environment.get("bigquery.credentials.private-key", String.class)
            .orElse(""))
            .replace("\\n", "\n");
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public void setPrivateKeyId(String privateKeyId) {
        this.privateKeyId = privateKeyId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public static String decryptKMSIfRequired(String encrypted) {
        // for local development
        if (encrypted.contains("-----BEGIN PRIVATE KEY-----")) {
            return encrypted;
        }


        AWSKMS kms = AWSKMSClientBuilder.standard().build();

        byte[] cipherBytes = Base64.decode(encrypted);

        DecryptRequest request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(cipherBytes));
        DecryptResult response = kms.decrypt(request);

        return new String(response.getPlaintext().array(), StandardCharsets.UTF_8);
    }

    public BigQuery createInstance() {
        try {
            URI tokenUri = new URI("https://oauth2.googleapis.com/token");

            GoogleCredentials credentials;

            credentials = ServiceAccountCredentials.newBuilder()
                .setClientId(clientId)
                .setClientEmail(clientEmail)
                .setPrivateKey(privateKeyFromPkcs8(decryptedPrivateKey))
                .setPrivateKeyId(privateKeyId)
                .setTokenServerUri(tokenUri)
                .setProjectId(projectId)
                .build();


            return BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible to instantiate a service account with current parameters", e);
        }
    }

    /** Helper to convert from a PKCS#8 String to an RSA private key */
    private static PrivateKey privateKeyFromPkcs8(String privateKeyPkcs8) throws IOException {
        // from ServiceAccountCredentials
        Reader reader = new StringReader(privateKeyPkcs8);
        PemReader.Section section = PemReader.readFirstSectionAndClose(reader, "PRIVATE KEY");
        if (section == null) {
            throw new IOException("Invalid PKCS#8 data.");
        }
        byte[] bytes = section.getBase64DecodedBytes();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        Exception unexpectedException;
        try {
            KeyFactory keyFactory = SecurityUtils.getRsaKeyFactory();
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            unexpectedException = exception;
        }
        throw new IOException("Unexpected exception reading PKCS#8 data", unexpectedException);
    }
}
