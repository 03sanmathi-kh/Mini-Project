package edu.kle.permits;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Passwords {
 public static final int MIN_BYTES=8,MAX_BYTES=72;
 private Passwords(){}
 public static int bytes(String value){return value==null?0:value.getBytes(StandardCharsets.UTF_8).length;}
 public static boolean valid(String value){int length=bytes(value);return length>=MIN_BYTES&&length<=MAX_BYTES;}
 public static void require(String value){if(!valid(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be 8–72 UTF-8 bytes.");}
}
