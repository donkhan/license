package license;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Properties;

import javax.crypto.NoSuchPaddingException;

import org.json.JSONObject;

import encryption.EncryptLicenseContent;

public class LicenseServer {
	private Selector selector;
	private InetSocketAddress listenAddress;
    private OutputStream logStream;
    
    public static void main(String[] args) throws Exception {
    	new LicenseServer("localhost", 8090).startServer();
    }

    public LicenseServer(String address, int port) throws IOException {
    	listenAddress = new InetSocketAddress(address, port);
    	File file = new File(System.getProperty("user.dir") + "\\" + "licenseserver.log");
    	if(!file.exists()){
    		file.createNewFile();
    	}
    	logStream = new FileOutputStream(file);
    }

    // create server channel	
    private void startServer() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // retrieve server socket and bind to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
        	public void start(){
        		try {
					logStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        });
        logStream.write("Server started...\n".getBytes());
        while (true) {
            // wait for events
            this.selector.select();
            //work on selected keys
            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                // this is necessary to prevent the same key from coming up 
                // again the next time around.
                keys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    this.accept(key);
                }
                else if (key.isReadable()) {
                    this.read(key);
                }
            }
        }
    }

    //accept a connection made to this channel's socket
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        logStream.write(("Connected to: " + remoteAddr + "\n").getBytes());
        channel.register(this.selector, SelectionKey.OP_READ);
    }
    
    //read from the socket channel
    private void read(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try{
        	int numRead = channel.read(buffer);
        	if (numRead == -1) {
        		Socket socket = channel.socket();
        		SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        		logStream.write(("Connection closed by client: " + remoteAddr + "\n").getBytes());
        		channel.close();
        		key.cancel();
        		return;
        	}
        	byte[] data = new byte[numRead];
            System.arraycopy(buffer.array(), 0, data, 0, numRead);
            String clientContent = new String(data);
            String licenseContent = process(clientContent);
            
            buffer.clear();
            channel.write(ByteBuffer.wrap(licenseContent.getBytes()));
        }catch(IOException ioe){
        	key.cancel();
        }

        
    }
    
    private String process(String clientContent) throws IOException{
    	JSONObject json = new JSONObject(clientContent);
    	String companyName = json.getString("company");
    	String productName = json.getString("product");
    	JSONObject license = new JSONObject();
    	ClassLoader classLoader = getClass().getClassLoader();
    	URL url = classLoader.getResource(productName + "/" + companyName + "license");
    	if(url == null) {
    		logStream.write(("License for Company " + companyName + " is not found\n").getBytes());
    		url = classLoader.getResource(productName + "/" + "default.license");
    	}
    	if(url == null){
    		logStream.write(("We are not supporting a product called " + productName + "\n").getBytes());
    		return license.toString();
    	}
    	Properties props = new Properties();
    	props.load(url.openStream());
    	license.put("allowedNoOfBranches",Integer.parseInt(props.getProperty("noOfBranches")));
    	license.put("expiryDate", props.getProperty("expiryDate"));
    	license.put("moosLicense",props.getProperty("moosLicense"));
    	String licenseContent = license.toString();
    	logStream.write((licenseContent+"\n").getBytes());
    	try {
			licenseContent = new EncryptLicenseContent().encryptText(licenseContent);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return licenseContent;
    }
}