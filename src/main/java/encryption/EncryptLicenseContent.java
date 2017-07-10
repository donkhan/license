package encryption;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class EncryptLicenseContent {
	private Cipher cipher;

	public EncryptLicenseContent() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("RSA");
	}

	private PrivateKey getPrivate() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
    	URL url = classLoader.getResource("KeyPair\\privateKey");
		byte[] keyBytes = getBytes(url);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	private byte[] getBytes(URL url) throws IOException {
		InputStream stream = url.openStream();
		int c = 0;
		List<Byte> list = new ArrayList<Byte>();
		while((c = stream.read()) != -1){
			list.add((byte)c);
		}
		byte[] b = new byte[list.size()];
		for(int i = 0;i<list.size();i++){
			b[i] = list.get(i);
		}
		return b;
	}

	public String encryptText(String msg)
			throws Exception {
		PrivateKey key = getPrivate();
		this.cipher.init(Cipher.ENCRYPT_MODE, key);
		return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

}