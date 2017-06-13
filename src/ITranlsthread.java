import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by user on 2017/6/12.
 */
public class ITranlsthread extends Thread{
    protected DatagramChannel channel;
    protected final ByteBuffer buffer = ByteBuffer.allocate(1+8+1024);
    public ITranlsthread(int port){
        try {
            InetSocketAddress local = new InetSocketAddress(port);
            this.channel = DatagramChannel.open().bind(local);
            this.channel.configureBlocking(false);
//            this.channel.socket().setSoTimeout(30 * 1000);
//            this.channel.socket().setReceiveBufferSize(1024 * 1024 * 1024);
//            this.channel.socket().setSendBufferSize(1024 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void log(Object object){
        System.err.println(object);
    }












}
