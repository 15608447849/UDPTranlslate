import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by user on 2017/6/13.
 */
public class Download2  extends ITranlsthread implements CompletionHandler<Integer,Void>{
    InetSocketAddress toAddress;
    String filePath = "C:\\FileServerDirs\\temp\\def.mp4";
    String filePathTemp = "C:\\FileServerDirs\\temp\\def.mp4.temp";
    Path temp = Paths.get(filePathTemp);
    AsynchronousFileChannel fileChannel;
    public Download2(int port, InetSocketAddress toAddress){
        super(port);
        this.toAddress = toAddress;
        startWork();
    }

    private void startWork() {
        try {
            if(!Files.exists(temp)){
                Files.createFile(temp);
            }
            fileChannel = AsynchronousFileChannel.open(temp, StandardOpenOption.WRITE);


            try {
                synchronized (this){
                    this.wait(1000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.clear();
            buffer.put((byte)1);
            buffer.flip();
            channel.send(buffer,toAddress);
            channel.connect(toAddress);
            log("请求已发送. - " + channel.isConnected());
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            loopDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loopDownload() throws IOException {
        log("请求下载: "+temp );
        long time = System.currentTimeMillis();
        ByteBuffer buf;
        long i = 0L;
        byte command;
        long recCount = 0L;
        long pos=0L;
        long filesize = -1L;
        while (true){
            buf = ByteBuffer.allocate(1+8+1024);
            buf.clear();
            long len = channel.read(buf);
            if (  len > 0) {
                buf.flip();
                command = buf.get(0);
                log(command);
                if (command==1 && filesize==-1){
                    buf.position(1);
                    //文件信息
                    log("文件大小: "+(filesize = buf.getLong()));
                    buffer.clear();
                    buffer.put((byte)1);
                    buffer.flip();
                    channel.write(buffer);
                }
                else if (command == 99){

                    //数据传输
                    buf.position(1);
                    long sendCount = buf.getLong();
                    if (sendCount==recCount){
                        buf.position(9);
//                        log("接受数据 - "+buf);
                        //接受数据
                        fileChannel.write(buf, pos,null,this);
                        pos+=(buf.limit()-9);
                        //回执
                        buffer.clear();
                        recCount++;
                        buffer.putLong(recCount);
                        buffer.flip();
//                        log("发送数据 - "+buffer);
                        channel.write(buffer);
                    }
                }
                else if (command == 100){
                    log("接受数据完成.");
                    break;
                    }
            }
        }

        log("下载结束,用时:"+(System.currentTimeMillis() - time)+"文件大小: "+temp.toFile().length()+" 文件MD5:"+ MD5Util.getFileMD5String(temp.toFile()) +" 接受次数:"+i);

    }

    long pos = 0L;
    @Override
    public void completed(Integer integer, Void aVoid) {
        log("当前成功写入:"+integer+",总进度:"+ (pos +=integer));
    }

    @Override
    public void failed(Throwable throwable, Void aVoid) {
            throwable.printStackTrace();
    }
}
