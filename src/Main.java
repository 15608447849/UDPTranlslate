import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Main {

    public static void main(String[] args) {
        try {
//            new Upload(8050).start();
            new Download2(6200,new InetSocketAddress("172.16.0.200",8050));
//            System.out.println(MD5Util.getFileMD5String(new File("C:\\FileServerDirs\\temp\\def.mp4")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
