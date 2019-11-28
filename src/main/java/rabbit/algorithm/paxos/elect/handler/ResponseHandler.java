package rabbit.algorithm.paxos.elect.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.algorithm.paxos.elect.DataHandler;
import rabbit.algorithm.paxos.elect.LeaderElector;
import rabbit.algorithm.paxos.protocol.ProtocolData;
import rabbit.algorithm.paxos.protocol.Result;
import rabbit.algorithm.paxos.protocol.Subject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 响应处理器
 * @author xiaoqianbin
 * @date 2019/11/26
 **/
public class ResponseHandler implements DataHandler {

    private LeaderElector elector;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public ResponseHandler(LeaderElector elector) {
        this.elector = elector;
    }

    @Override
    public void process(ProtocolData data) {
        if (null != data.getReceiver() && !elector.getServer().getServerId().equals(data.getReceiver())) {
            // 这个包不是给我的直接忽略
            return;
        }

        if (Subject.ELECT_LEADER == data.getSubject()) {
            AtomicLong localVersion = elector.getVersionBySubject(data.getSubject());
            if (data.getVersion() == localVersion.get() && Result.AGREE == data.getResult()) {
                logger.info("'{}' ELECT_LEADER({}) is agreed by '{}'!", elector.getServer().getServerId(), data.getVersion(), data.getSenderId());
                elector.incrementAgreedTickets();
            } else {
                logger.info("'{}' ELECT_LEADER({}) is rejected by '{}'", elector.getServer().getServerId(), data.getVersion(), data.getSenderId());
            }
        }

        if (Subject.BROADCAST_LEADER == data.getSubject()) {
           logger.info("node '{}' joined us, node id is '{}'", data.getHost(), data.getSenderId());
        }
    }
}
