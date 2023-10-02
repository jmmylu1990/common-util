package org.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class CryptUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CryptUtils.class);

	public static final String MD5_ALGORITHM = "MD5";
	public static final String SHA_ALGORITHM = "SHA";
	public static final String SHA1_ALGORITHM = "SHA-1";
	public static final String SHA256_ALGORITHM = "SHA-256";
	public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
	public static final String AES_ALGORITHM = "AES";
	public static final String RSA_ALGORITHM = "RSA";
	
	private static final Base64.Encoder BASE64_URL_SAFE_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();
//	private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

	private CryptUtils() {
	}
	
	public static String encryptWithHash(String str, final String algorithm) {
		MessageDigest messageDigest;
		String encodeStr = null;
		try {
			messageDigest = MessageDigest.getInstance(algorithm);
			messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
			encodeStr = parseByte2Hex(messageDigest.digest());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return encodeStr;
	}

	public static String encryptWithHash(String content) {
		MessageDigest messageDigest;
		String encodeStr = null;
		try {
			messageDigest = MessageDigest.getInstance(SHA256_ALGORITHM);
			messageDigest.update(content.getBytes(StandardCharsets.UTF_8));
			encodeStr = parseByte2Hex(messageDigest.digest());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return encodeStr;
	}

	public static String encryptWithAES(String content, String privateKey) {
		try {
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
			byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
			cipher.init(Cipher.ENCRYPT_MODE, keyGeneratorForAES(privateKey));
			byte[] result = cipher.doFinal(byteContent);

			return parseByte2Hex(result);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}

	public static String decryptWithAES(String content, String privateKey) {
		try {
			byte[] decryptFrom = parseHexStr2Byte(content);
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, keyGeneratorForAES(privateKey));
			byte[] result = cipher.doFinal(decryptFrom);

			return new String(result);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	private static SecretKeySpec keyGeneratorForAES(String privateKey) {
		byte[] enCodeFormat = new byte[1];
		try {
			KeyGenerator kgen = KeyGenerator.getInstance(AES_ALGORITHM);
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(privateKey.getBytes());
			kgen.init(128, secureRandom);
			SecretKey secretKey = kgen.generateKey();
			enCodeFormat = secretKey.getEncoded();

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return new SecretKeySpec(enCodeFormat, AES_ALGORITHM);
	}

   public static void generateKeyForRSA() {
        try (ObjectOutputStream publicKeyOut = new ObjectOutputStream(new FileOutputStream("public_key.dat"));
        		ObjectOutputStream privateKeyOut = new ObjectOutputStream(new FileOutputStream("private_key.dat"))) {
            KeyPairGenerator kpGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            kpGen.initialize(2048);
            KeyPair kp = kpGen.genKeyPair();
            PublicKey pbkey = kp.getPublic();
            PrivateKey prkey = kp.getPrivate();

            publicKeyOut.writeObject(pbkey);
            privateKeyOut.writeObject(prkey);
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(), e);
        }
    }
  
   public static String encryptWithRSA(String content, String publickKeyPath) 
		   throws IOException, ClassNotFoundException {
	   try (ObjectInputStream publicKeyIn = new ObjectInputStream(new FileInputStream(publickKeyPath))) {
		   RSAPublicKey publicKey = (RSAPublicKey) publicKeyIn.readObject();
		   BigInteger e = publicKey.getPublicExponent();
		   BigInteger n = publicKey.getModulus();
		  
		   byte[] ptext = content.getBytes(StandardCharsets.UTF_8);
		   BigInteger m = new BigInteger(ptext);
		   BigInteger c = m.modPow(e, n);

			return BASE64_URL_SAFE_ENCODER.encodeToString(c.toByteArray());
		}
   }
  
   public static String decryptWithRSA(String content, String privateKeyPath) throws IOException, ClassNotFoundException {
	   try (ObjectInputStream b = new ObjectInputStream(new FileInputStream(privateKeyPath))) {
		   BigInteger c = new BigInteger(Base64.getUrlDecoder().decode(content));
		   RSAPrivateKey privateKey = (RSAPrivateKey) b.readObject();
		   BigInteger d = privateKey.getPrivateExponent();
		   BigInteger n = privateKey.getModulus();
		   BigInteger m = c.modPow(d, n);
		  
		   return new String(m.toByteArray(), StandardCharsets.UTF_8);
		}
   }
   
   public static String encryptWithHMAC(String content, String privateKey) {
		try {
			// Get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);

			// Get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
			
			return BASE64_ENCODER.encodeToString(rawHmac);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}
   
	private static String parseByte2Hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		String temp = null;
		for (int i = 0; i < bytes.length; i++) {
			temp = Integer.toHexString(bytes[i] & 0xFF);
			if (temp.length() == 1) {
				sb.append("0");
			}
			sb.append(temp);
		}
		return sb.toString();
	}
	
	private static byte[] parseHexStr2Byte(String hexStr) {
		if (hexStr.length() < 1) return new byte[0];

		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte) (high * 16 + low);
		}

		return result;
	}
}