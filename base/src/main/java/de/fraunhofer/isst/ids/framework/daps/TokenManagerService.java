/*-
 * ========================LICENSE_START=================================
 * ids-token-manager
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
 * %%
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
 * =========================LICENSE_END==================================
 */
package de.fraunhofer.isst.ids.framework.daps;

import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

/**
 * Manages Dynamic Attribute Tokens.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
public class TokenManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenManagerService.class);

    private static SSLSocketFactory sslSocketFactory = null;

    /**
     * Get the DAT from the DAPS at dapsURL using the current configuration
     *
     * @param container An IDS Connector Configuration
     * @param dapsUrl The URL of a DAPS Service
     * @param provider providing underlying OkHttpClient
     * @return signed DAPS JWT token for the Connector
     */
    public static String acquireToken(ConfigurationContainer container, ClientProvider provider, String dapsUrl) {

            var keyStoreManager = container.getKeyManager();

            String dynamicAttributeToken = "INVALID_TOKEN";
            String targetAudience = "idsc:IDS_CONNECTORS_ALL";

            // Try clause for setup phase (loading keys, building trust manager)
            try {

                // get private key
                LOG.debug("Getting PrivateKey and Certificate from KeyStoreManager");
                Key privKey = keyStoreManager.getPrivateKey();
                // Get certificate of public key
                X509Certificate cert = (X509Certificate) keyStoreManager.getCert();

                // Get AKI
                //GET 2.5.29.14	SubjectKeyIdentifier / 2.5.29.35	AuthorityKeyIdentifier
                LOG.debug("Get AKI from certificate");
                String aki_oid = Extension.authorityKeyIdentifier.getId();
                byte[] rawAuthorityKeyIdentifier = cert.getExtensionValue(aki_oid);
                if(rawAuthorityKeyIdentifier == null){
                    throw new MissingCertExtensionException("AKI of the Connector Certificate is null!");
                }
                ASN1OctetString akiOc = ASN1OctetString.getInstance(rawAuthorityKeyIdentifier);
                AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(akiOc.getOctets());
                byte[] authorityKeyIdentifier = aki.getKeyIdentifier();

                //GET SKI
                LOG.debug("Get SKI from certificate");
                String ski_oid = Extension.subjectKeyIdentifier.getId();
                byte[] rawSubjectKeyIdentifier = cert.getExtensionValue(ski_oid);
                if(rawSubjectKeyIdentifier == null){
                    throw new MissingCertExtensionException("SKI of the Connector Certificate is null!");
                }
                ASN1OctetString ski0c = ASN1OctetString.getInstance(rawSubjectKeyIdentifier);
                SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(ski0c.getOctets());
                byte[] subjectKeyIdentifier = ski.getKeyIdentifier();

                String aki_result = beautifyHex(encodeHexString(authorityKeyIdentifier).toUpperCase());
                String ski_result = beautifyHex(encodeHexString(subjectKeyIdentifier).toUpperCase());

                String connectorUUID = ski_result + "keyid:" + aki_result.substring(0, aki_result.length() - 1);

                LOG.info("ConnectorUUID: " + connectorUUID);
                LOG.info("Retrieving Dynamic Attribute Token...");


                // create signed JWT (JWS)
                // Create expiry date one day (86400 seconds) from now
                LOG.debug("Building jwt token");
                Date expiryDate = Date.from(Instant.now().plusSeconds(86400));
                JwtBuilder jwtb =
                        Jwts.builder()
                                .setIssuer(connectorUUID)
                                .setSubject(connectorUUID)
                                .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                                .claim("@type", "ids:DatRequestToken")
                                .setExpiration(expiryDate)
                                .setIssuedAt(Date.from(Instant.now().minusSeconds(10)))
                                .setAudience(targetAudience)
                                .setNotBefore(Date.from(Instant.now().minusSeconds(10)));

                LOG.debug("Signing jwt token");
                String jws = jwtb.signWith(SignatureAlgorithm.RS256, privKey).compact();
                LOG.info("Request token: " + jws);

                // build form body to embed client assertion into post request
                RequestBody formBody =
                        new FormBody.Builder()
                                .add("grant_type", "client_credentials")
                                .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                                .add("client_assertion", jws)
                                .add("scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
                                .build();

                LOG.debug("Getting idsutils client");
                var client = provider.getClient();

                Request request = new Request.Builder().url(dapsUrl + "/v2/token").post(formBody).build();

                LOG.debug(String.format("Sending request to %s", dapsUrl+"/v2/token"));
                Response jwtResponse = client.newCall(request).execute();
                if (!jwtResponse.isSuccessful()) {
                    LOG.debug("DAPS request was not successful");
                    throw new IOException("Unexpected code " + jwtResponse);
                }
                var responseBody = jwtResponse.body();
                if (responseBody == null) {
                    throw new EmptyDapsResponseException("JWT response is null.");
                }
                var jwtString = responseBody.string();
                LOG.info("Response body of token request:\n{}", jwtString);

                JSONObject jsonObject = new JSONObject(jwtString);
                dynamicAttributeToken = jsonObject.getString("access_token");

                LOG.info("Dynamic Attribute Token: " + dynamicAttributeToken);

            }catch (IOException e) {
                LOG.error(String.format("Error retrieving token: %s", e.getMessage()));
            }catch (EmptyDapsResponseException e) {
                LOG.error(String.format("Something else went wrong: %s", e.getMessage()));
            }catch (MissingCertExtensionException e){
                LOG.error("Certificate of the Connector is missing aki/ski extensions!");
            }
            return dynamicAttributeToken;
    }

    /***
     * Beautyfies Hex strings and will generate a result later used to create the client id (XX:YY:ZZ)
     *
     * @param hexString HexString to be beautified
     * @return beautifiedHex result
     */
    private static String beautifyHex(String hexString) {
        return Arrays.stream(split(hexString,2))
                .map(s -> s + ":")
                .collect(Collectors.joining());
    }

    /***
     * Split string every n chars and return string array
     *
     * @param src a string that will be split into multiple substrings
     * @param n number of chars per resulting string
     * @return Array of strings resulting from splitting the input string every n chars
     */
    public static String[] split(String src, int n) {
        String[] result = new String[(int)Math.ceil((double)src.length()/(double)n)];
        for (int i=0; i<result.length; i++)
            result[i] = src.substring(i*n, Math.min(src.length(), (i+1)*n));
        return result;
    }

}