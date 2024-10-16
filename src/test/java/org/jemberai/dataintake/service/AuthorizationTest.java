package org.jemberai.dataintake.service;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 18/09/2024.
 */

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;


public class AuthorizationTest {
    private static final String JEMBER_CLIENT_ID = "jember-client";
    private static final String JEMBER_CLIENT_SECRET = "jember";

    @Test
    public void testAuthorizationClientSecret() {
        String authorization = Base64.getEncoder().encodeToString((JEMBER_CLIENT_ID + ":" + JEMBER_CLIENT_SECRET).getBytes());
        System.out.println("Authorization: " + authorization);
        Assertions.assertEquals("amVtYmVyLWNsaWVudDpqZW1iZXI=", authorization);
    }
}
