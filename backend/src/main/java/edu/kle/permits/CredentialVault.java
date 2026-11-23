package edu.kle.permits;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Seals Admin-created login passwords so that only the applicant's registration password can reveal them.
 * Registration: RSA-2048 key pair; private key encrypted with AES-256-GCM under PBKDF2(registration password).
 * Approval: login password encrypted with the public key (RSA-OAEP-SHA256). The server alone cannot decrypt it.
 */
final class CredentialVault {
 private CredentialVault(){}
 private static final int ITERATIONS=120_000;
 private static final String OAEP="RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
 private static final SecureRandom RANDOM=new SecureRandom();
 private static final Base64.Encoder B64=Base64.getEncoder();private static final Base64.Decoder D64=Base64.getDecoder();
 record Keys(String publicKey,String encryptedPrivateKey,String salt,String iv){}

 static Keys create(String registrationPassword){
  try{
   KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048,RANDOM);KeyPair pair=g.generateKeyPair();
   byte[] salt=random(16),iv=random(12);
   Cipher aes=Cipher.getInstance("AES/GCM/NoPadding");aes.init(Cipher.ENCRYPT_MODE,kek(registrationPassword,salt),new GCMParameterSpec(128,iv));
   byte[] encrypted=aes.doFinal(pair.getPrivate().getEncoded());
   return new Keys(B64.encodeToString(pair.getPublic().getEncoded()),B64.encodeToString(encrypted),B64.encodeToString(salt),B64.encodeToString(iv));
  }catch(GeneralSecurityException e){throw new IllegalStateException("Credential key generation failed",e);}
 }
 static String seal(String publicKey,String secret){
  try{
   PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(D64.decode(publicKey)));
   Cipher rsa=Cipher.getInstance(OAEP);rsa.init(Cipher.ENCRYPT_MODE,key,new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));
   return B64.encodeToString(rsa.doFinal(secret.getBytes(StandardCharsets.UTF_8)));
  }catch(GeneralSecurityException e){throw new IllegalStateException("Credential sealing failed",e);}
 }
 /** Returns null if the registration password does not unlock the private key. */
 static String open(String registrationPassword,String encryptedPrivateKey,String salt,String iv,String sealed){
  try{
   Cipher aes=Cipher.getInstance("AES/GCM/NoPadding");aes.init(Cipher.DECRYPT_MODE,kek(registrationPassword,D64.decode(salt)),new GCMParameterSpec(128,D64.decode(iv)));
   PrivateKey key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(aes.doFinal(D64.decode(encryptedPrivateKey))));
   Cipher rsa=Cipher.getInstance(OAEP);rsa.init(Cipher.DECRYPT_MODE,key,new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));
   return new String(rsa.doFinal(D64.decode(sealed)),StandardCharsets.UTF_8);
  }catch(GeneralSecurityException|IllegalArgumentException e){return null;}
 }
 private static SecretKey kek(String password,byte[] salt)throws GeneralSecurityException{
  PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,ITERATIONS,256);
  try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}finally{spec.clearPassword();}
 }
 private static byte[] random(int n){byte[] b=new byte[n];RANDOM.nextBytes(b);return b;}
}
