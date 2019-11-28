package rabbit.algorithm.paxos.elect;

import java.net.DatagramPacket;

/**
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
@FunctionalInterface
public interface PacketHandler {

    void process(DatagramPacket packet);

}
