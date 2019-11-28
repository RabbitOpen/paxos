package rabbit.algorithm.paxos.elect;

import rabbit.algorithm.paxos.protocol.ProtocolData;

/***
 * 包处理接口
 * @author  xiaoqianbin
 * @date    2019/11/26
 **/
@FunctionalInterface
public interface DataHandler {
    void process(ProtocolData data);
}
