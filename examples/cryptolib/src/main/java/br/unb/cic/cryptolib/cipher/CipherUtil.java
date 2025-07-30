package br.unb.cic.cryptolib.cipher;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class CipherUtil {

	public void encryptUnreachable(String input) {
		try {
			Cipher.getInstance("DES");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
