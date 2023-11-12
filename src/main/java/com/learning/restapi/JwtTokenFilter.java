package com.learning.restapi;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.learning.restapi.api.service.JwtVerificationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private JwtVerificationService jwtServices;

    public JwtTokenFilter(JwtVerificationService jwtServices) {
        this.jwtServices = jwtServices;
    }

protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    String token = extractTokenFromRequest(request);

    if (token != null) {
        try {
            // Verify the token
            validateToken(token);
            System.out.println("validated");
            // If the token is valid, pass control to the endpoint handler.
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Token is not valid, return an unauthorized response
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: " + e.getMessage()); // Customize this error message
        }
    } else {
        // Token is missing, return an unauthorized response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized: Token is missing");
    }
}


    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Remove "Bearer " to get the token.
        }

        return null; // No token found in the header.
    }

    private Boolean validateToken(String tokenWithoutBearer) throws Exception {

        try{
            DecodedJWT jwt = JWT.decode(tokenWithoutBearer);
            String keyId = jwt.getKeyId();
            RSAPublicKey publicKey = jwtServices.getPublicKey("https://www.googleapis.com/oauth2/v3/certs",keyId);
            Algorithm algorithm = Algorithm.RSA256(publicKey);

            JWTVerifier verifier = JWT.require(algorithm).withIssuer(jwt.getIssuer()).build();

            DecodedJWT resp = verifier.verify(jwt);
            return Boolean.TRUE;
        }catch (Exception e){
            System.out.println("message" + e.getMessage());
//            return Boolean.FALSE;
            throw e;
        }

    }
}

