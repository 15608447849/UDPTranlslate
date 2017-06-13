import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

/**
 * Created by user on 2017/6/12.
 */
public class Upload extends ITranlsthread {


    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            InetSocketAddress broadAddress =  new InetSocketAddress("255.255.255.255",9000);
            try {
                DatagramChannel broadChannel = DatagramChannel.open().bind(new InetSocketAddress(9001));
                broadChannel.configureBlocking(false);
            } catch (IOException e) {
                return;
            }
            while (channel.isOpen()){
                if (!channel.isConnected()){
                    //发送局域网广播
                    ByteBuffer buf = ByteBuffer.allocate(4); //端口号
                    buf.clear();
                    buf.putInt(8050);
                    try {
                        buf.flip();
                        channel.send(buf,broadAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (this){
                    try {
                        this.wait(2*1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    };

    public Upload(int port) throws IOException {
        super(port);
        new Thread().start();
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
            log("上传文件: " + filePath + " 大小:"+fileSize +"文件MD5: "+MD5Util.getFileMD5String(new File(filePath)) +" - " + channel.getRemoteAddress() +" - "+channel.isConnected());

            buffer.clear();
            buffer.put((byte)1);
            buffer.putLong(fileSize);
            buffer.flip();
            ByteBuffer recBuf = ByteBuffer.allocate(8);
            while (true){
                //发送文件大小,文件MD5.
               buffer.rewind();
               channel.write(buffer);
                recBuf.clear();
                long len = channel.read(recBuf);
                if (len>0){
                    log("开始上传.");
                    break;
                }
            }



            MappedByteBuffer fileBytebuffer = inChannel.map(READ_ONLY,0,fileSize);

            boolean isReadable = true;
            int overTimeCount = 0;
            while (fileBytebuffer.hasRemaining()){
                if (isReadable){
                    buffer.clear();
                    buffer.put((byte)99);//数据传输
                    buffer.putLong(sendCount);//当前传输次数.
                    while (fileBytebuffer.hasRemaining() && buffer.hasRemaining()){
                        buffer.put(fileBytebuffer.get());
                    }
                    buffer.flip();
                    sendCount++;//传输次数
//                    log("发送 - "+buffer +" 当前次数:"+sendCount);
                    isReadable = false;
                }else{
                    buffer.rewind();
                }
                channel.write(buffer);

                //接受
                recBuf.clear();
                long len = channel.read(recBuf);
                if (len>0 ){
                    recBuf.flip();
                    if (recBuf.limit()==8 && recBuf.getLong() == sendCount){
                        overTimeCount=0;
                        isReadable = true;
                    }
                }else{
                    if (overTimeCount == 300) {
                        log("传输超时.");
                       break;
                    }else{
                        TimeUnit.MICROSECONDS.sleep(10);
                        overTimeCount++;//超时时间计数 10 * 3000次 = 30000纳秒
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

//               if ((sendCount&1) != 0){
//                   synchronized (this){
//                       this.wait(100);
//                   }

//               }
//               log("send count: "+ sendCount);