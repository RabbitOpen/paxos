package rabbit.algorithm.paxos.elect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * udp服务
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class UdpServer extends Thread {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private MulticastSocket ds;

    private PacketHandler dataHandler;

    private boolean run = true;

    private String serverId;

    public UdpServer(PacketHandler dataHandler, int port, String multicastHost) {
        try {
            setName("udp-server-thread-" + getId());
            this.dataHandler = dataHandler;
            ds = new MulticastSocket(port);
            InetAddress receiveAddress = InetAddress.getByName(multicastHost);
            ds.joinGroup(receiveAddress);
            serverId = UUID.randomUUID().toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        DatagramPacket dp = new DatagramPacket(buffer.array(), buffer.capacity());
        while (run) {
            try {
                ds.receive(dp);
                dataHandler.process(dp);
                if ("hello".equals(new String(dp.getData(), 0, dp.getLength()))) {
                    send(new DatagramPacket("hello to".getBytes(), 0, "hello to".length(), InetAddress.getByName("227.0.0.1"), 9999));
                }
                buffer.clear();
            } catch (Exception e) {
                logger.info(String.format("MulticastSocket is closed！ %s", e.getMessage()));
            }
        }
    }

    /**
     * 发送数据
     * @param	packet
     * @author  xiaoqianbin
     * @date    2019/11/25
     **/
    public void send(DatagramPacket packet) throws IOException {
        ds.send(packet);
    }

    public void close() {
        run = false;
        ds.close();
        try {
            join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("server is closed！");
    }

    public String getServerId() {
        return serverId;
    }

}
