import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

/**
 * Created by user on 2017/6/12.
 */
public class Upload extends ITranlsthread {

    public Upload(int port) throws IOException {
        super(port);
    }

    @Override
    public void run() {
        while (true){
            work();
        }
    }

    private void work() {
        log("等待下载者接入.");
        while (true){
            buffer.clear();
            try {
                SocketAddress toAddress = channel.receive(buffer);
                if (toAddress!=null){

                    buffer.flip();
                    if (buffer.get(0) == 1){
                        log("接入 - "+ toAddress);
                        this.channel.connect(toAddress);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            synchronized (this) {
                this.wait(1000);
            }
            long sendCount = 0L;
            String filePath = "C:\\FileServerDirs\\source\\def.mp4";
            FileChannel inChannel = new RandomAccessFile(filePath,"rw").getChannel();
            long fileSize = inChannel.size();
            log("上传文件: " + filePath + " 大小:"+fileSize);
            MappedByteBuffer fileBytebuffer = inChannel.map(READ_ONLY,0,fileSize);

            long pos = 0L;
            long perpos = pos;
            int loopCount = 0 ;
            while (fileBytebuffer.hasRemaining()){
                buffer.clear();
                buffer.position(9);
                while (fileBytebuffer.hasRemaining() && buffer.hasRemaining()){
                    buffer.put(fileBytebuffer.get());
                    pos++;
                }

                buffer.position(0);
                buffer.put((byte)99);
                buffer.putLong(perpos);//下载保存点
                buffer.position(buffer.limit());

                buffer.flip();
                channel.write(buffer);
                perpos = pos;//上一次
                sendCount++;
                loopCount++;
//                if (loopCount==3){
//                    loopCount = 0;
//                    synchronized (this){
//                       this.wait(10);
//                   }
//                }
               if ((sendCount&1) != 0){
                   synchronized (this){
                       this.wait(10);
                   }
               }
            }
            inChannel.close();
            new MPrivilegedAction(fileBytebuffer);

            log("发送次数: "+sendCount);
            log("关闭本地流.");
            //发送结束标识符号
            buffer.clear();
            buffer.put((byte)100);
            buffer.putLong(sendCount);
            buffer.flip();
            channel.write(buffer);
            log("已发送结束标识符");
            int i = 0;
            while (channel.isConnected() && i<100){
                buffer.clear();
                long len = channel.read(buffer);
                while (len>0){
                    break;
                }
                synchronized (this){
                    this.wait(10);
                }
                i++;
            }
            log("上传完成.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (channel.isConnected()) {
                try {
                    log("断开 - " + channel.getRemoteAddress());
                    channel.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
