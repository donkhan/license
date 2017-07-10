package license;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class DecryptLicenseContent {
	private Cipher cipher;

	public DecryptLicenseContent() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("RSA");
	}

	private PublicKey getPublic() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
    	URL url = classLoader.getResource("KeyPair\\publicKey");
		byte[] keyBytes = getBytes(url);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
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

	public String decryptText(String msg)
			throws Exception {
		PublicKey key = getPublic();
		this.cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(Base64.getDecoder().decode(msg.getBytes())), "UTF-8");
	}

}