package license;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class LicenseServer {
	private Selector selector;
	private InetSocketAddress listenAddress;
    private OutputStream logStream;
    
    public static void main(String[] args) throws Exception {
    	System.out.println("MEain Function Called");
    	new LicenseServer("192.168.0.15", 8090).startServer();
    }

    public LicenseServer(String address, int port) throws IOException {
    	listenAddress = new InetSocketAddress(address, port);
    	System.out.println(System.getProperty("user.dir"));
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
    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
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
        buffer.clear();
        byte [] message = new String("{ product: \"MoneyBox\", company : \"MaxMoney Sdn Bhd.\", noofBranches: \"3\"}").getBytes();
        
        channel.write(ByteBuffer.wrap(message));
    }
}