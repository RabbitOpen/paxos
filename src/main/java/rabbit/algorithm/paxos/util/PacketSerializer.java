package rabbit.algorithm.paxos.util;

import rabbit.algorithm.paxos.protocol.ProtocolData;

import java.io.*;

/**
 * 包解析器(此处没有对包进行压缩)
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class PacketSerializer {

    /***
     * 序列化
     * @param	object
     * @author  xiaoqianbin
     * @date    2019/11/25
     **/
    public static byte[] serialize(ProtocolData object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.flush();
        byte[] bytes = bos.toByteArray();
        oos.close();
        bos.close();
        return bytes;
    }

    /**
     * 反序列化
     * @param	bytes
     * @author  xiaoqianbin
     * @date    2019/11/25
     **/
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object object = ois.readObject();
        ois.close();
        bis.close();
        return object;
    }

}
