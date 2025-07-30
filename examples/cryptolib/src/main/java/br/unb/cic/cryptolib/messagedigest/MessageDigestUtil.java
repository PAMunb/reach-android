package br.unb.cic.cryptolib.messagedigest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestUtil {

	public String hash(String input, String algorithm) {
		try {
			return hash(input.getBytes(), algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String hash(byte[] input, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
		messageDigest.update(input);
		byte[] digest = messageDigest.digest();
		return bytesToHex(digest);
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public String hashUnreachable(String input) {
		return hash(input, "MD5");
	}
}
