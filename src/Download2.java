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
        log("###");
        startWork();
    }

    private void startWork() {
        try {
            if(!Files.exists(temp)){
                Files.createFile(temp);
            }

            fileChannel = AsynchronousFileChannel.open(temp, StandardOpenOption.WRITE);
            channel.connect(toAddress);
            start();
            buffer.clear();
            buffer.put((byte)1);
            buffer.flip();
            channel.send(buffer,toAddress);
            log("请求已发送.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            log("___________________");
            loopDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loopDownload() throws IOException {
        log("正在下载: "+temp );
        long time = System.currentTimeMillis();
        long length;
        ByteBuffer buf;
        long i = 0L;
        while (true){
            //放入缓冲区,如果缓冲区满了->
            buf = ByteBuffer.allocate(1+8+1024);
            buf.clear();
            length = channel.read(buf);
            buf.flip();
            if ( length > 0) {
                if (buf.limit()>9){
                    buf.position(1);
                    long position = buf.getLong();
                    fileChannel.write(buf, position,null,this);
                    i++;
                }
                else {
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
