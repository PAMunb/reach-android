package br.unb.cic.cryptolib;

import br.unb.cic.cryptolib.messagedigest.MessageDigestUtil;

public class Main {

	public static void main(String[] args) {
		String input = "123";
		String alg = "SHA-256";

		String hash = new MessageDigestUtil().hash(input, alg);
		System.out.println(hash);
	}

}
