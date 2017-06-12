import javax.swing.text.html.parser.Entity;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by user on 2017/6/12.
 */
public class Download extends ITranlsthread {

    private InetSocketAddress toAddress;
    String filePath = "C:\\FileServerDirs\\temp\\def.mp4";
    String filePathTemp = "C:\\FileServerDirs\\temp\\def.mp4.temp";
    Path temp = Paths.get(filePathTemp);
    public Download(int port, InetSocketAddress toAddress) throws IOException {
        super(port);
        this.toAddress = toAddress;
    }
    long position = 0;
    long sendSize = -1L;
    long receiceCount=0L;
    long length;
    ByteBuffer buf;
    final ArrayList<Future<Integer>> list = new ArrayList<>();
    final ArrayList<readThread> listThread = new ArrayList<>();
    final HashMap<Future<Integer>,AsynchronousFileChannel> hashMap = new HashMap<Future<Integer>,AsynchronousFileChannel>();
//    AsynchronousFileChannel outChannel = null;

    @Override
    public void run() {

        try {
            channel.connect(toAddress);
            buffer.clear();
            buffer.put((byte)1);
            buffer.flip();
            channel.send(buffer,toAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //下载文件


            if(!Files.exists(temp)){
                Files.createFile(temp);
            }

//            outChannel = AsynchronousFileChannel.open(temp,StandardOpenOption.WRITE);
            long time = System.currentTimeMillis();
            log("正在下载: "+temp );

            while (true){
                ByteBuffer buf = ByteBuffer.allocate(1+8+1024);
                buf.clear();
                length = channel.read(buf);
                if ( length > 0) {
                    new readThread(buf);
                }
                if (sendSize>0) break;
            }


//            while (list.size()>0){
//                Iterator<Future<Integer>> itr = list.iterator();
//                Future<Integer> ops;
//                while (itr.hasNext()){
//                    ops=itr.next();
//                    if (ops==null) return;
//                    if (ops.isDone()){
//                        itr.remove();
//                    }
//                }
//            }
//            log("当前存在线程数:"+listThread.size());
//              while (listThread.size()>0){
//                Iterator<readThread> itr = listThread.iterator();
//                  readThread thread;
//                while (itr.hasNext()){
//                    thread = itr.next();
//                    if (thread.isSuccess){
//                        itr.remove();
//                    }
//                }
//            }

//            while (listThread.size()>0){log(listThread.size());}

            while (hashMap.size()>0){
                log("map size : "+ hashMap.size());
                try{
                    lock.lock();
                    Iterator<Map.Entry<Future<Integer>,AsynchronousFileChannel>> itr = hashMap.entrySet().iterator();
                    Map.Entry<Future<Integer>,AsynchronousFileChannel> entity;
                    while (itr.hasNext()){
                        entity = itr.next();
                        if (entity.getKey().isDone()){
                            try {
                                entity.getValue().close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                itr.remove();
                            }
                        }
                    }

                }finally {
                    lock.unlock();
                }


            }

            File file = temp.toFile();
            log("下载成功: " + temp + " ,文件大小: "+ file.length() +" 耗时:"+ (System.currentTimeMillis() - time ) +" 毫秒. 接受次数:"+receiceCount+" - 实际发送次数:"+sendSize);
//            outChannel.close();

//            log("outChannel.isOpen() == "+outChannel.isOpen()+" ,重命名 - "+ file.renameTo(new File(filePath)));
            log(" ,重命名 - "+ file.renameTo(new File(filePath)));

            buffer.clear();
            buffer.put((byte) 1);
            buffer.flip();
            channel.write(buffer);
            synchronized (this){
                this.wait(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private final ReentrantLock lock = new ReentrantLock();

    private class readThread extends Thread{
        private ByteBuffer buf;
        public volatile boolean isSuccess;
        public readThread(ByteBuffer buf) {
            this.buf = buf;
            receiceCount++;
            this.start();
        }

        @Override
        public void run() {
//            try{
//                lock.lock();
//                listThread.add(this);
//            }finally {
//                lock.unlock();
//            }
            AsynchronousFileChannel outChannel = null;
            try {
                outChannel = AsynchronousFileChannel.open(temp, StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buf.flip();
            if (buf.get(0) == 100){
                log("收到结束标识.");
                sendSize = buf.getLong(1);
//                break;
            }else{
                long pos = buf.getLong(1);
                if (position != pos){
//                    log("当前实际写入位置 : " + pos +" 记录位置 - "+position +" length == "+length +" buff: "+buf);
                }
                buf.position(9);
//                list.add();
                Future<Integer> ops = outChannel.write(buf, pos);
//                while (!ops.isDone());
                position += (buf.limit()-9);
                try {
                    outChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                log(this +" 写入完成. " + pos);
                try{
                    lock.lock();
                    hashMap.put(ops,outChannel);
                }finally {
                    lock.unlock();
                }
            }
            isSuccess = true;
        }
    }



}
