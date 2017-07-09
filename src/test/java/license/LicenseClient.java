package license;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class LicenseClient {
 
	public static void main(String args[]) throws IOException, InterruptedException{
		new LicenseClient().startClient();
	}
	
    public void startClient(){
 
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8091);
		try {
			SocketChannel  channel = SocketChannel.open(hostAddress);
			System.out.println("Client... started");
	        byte [] message = new String("{  product: \"MoneyBox\", company : \"MaxMoney Sdn Bhd.\" }").getBytes();
	        ByteBuffer buffer = ByteBuffer.wrap(message);
	        channel.write(buffer);
	        buffer.clear();
	        ByteBuffer rBuffer = ByteBuffer.allocate(1024*4);
	        int numRead = channel.read(rBuffer);
	        byte[] data = new byte[numRead];
	        System.arraycopy(rBuffer.array(), 0, data, 0, numRead);
	        System.out.println("From Server: " + new String(data));
	        channel.close();
		} catch (IOException e) {
		}
		            
    }
}

