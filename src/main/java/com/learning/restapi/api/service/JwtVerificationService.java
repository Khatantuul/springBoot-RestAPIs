package com.learning.restapi.api.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.stereotype.Service;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Service
public class JwtVerificationService {

    public RSAPublicKey getPublicKey(String jwksUri, String keyId) throws Exception {
        URL jwkSetURL = new URL(jwksUri);
        JWKSet jwkSet = JWKSet.load(jwkSetURL);

        JWK jwk = jwkSet.getKeyByKeyId(keyId);
        RSAPublicKey pub = jwk.toRSAKey().toRSAPublicKey();

        return pub;

    }


}
