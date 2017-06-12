import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Main {

    public static void main(String[] args) {
        try {
//            new Upload(8050).start();
            new Download(7000,new InetSocketAddress("172.16.0.200",8050)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
