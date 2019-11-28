package rabbit.algorithm.paxos.elect.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.algorithm.paxos.elect.DataHandler;
import rabbit.algorithm.paxos.elect.LeaderElector;
import rabbit.algorithm.paxos.protocol.*;
import rabbit.algorithm.paxos.util.PacketSerializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求处理器
 * @author xiaoqianbin
 * @date 2019/11/26
 **/
public class RequestHandler implements DataHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private LeaderElector elector;

    private Map<Subject, DataHandler> requestHandlers = new EnumMap<>(Subject.class);

    public RequestHandler(LeaderElector elector) {
        this.elector = elector;
        initPacketHandler();
    }

    private void initPacketHandler() {

        // 广播leader身份
        requestHandlers.put(Subject.BROADCAST_LEADER, (data) -> {
            if (null != elector.getLeader()) {
                if (!data.getSenderId().equals(elector.getLeader())) {
                    // 发现新的leader诞生了，但是旧的leader信息还在
                    logger.error("found new leader '{}', current leader is '{}'", data.getSenderId(), elector.getLeader());
                }
                return;
            }
            elector.endVote();
            // 同步选举的版本号
            elector.getVersionBySubject(Subject.ELECT_LEADER).set(data.getVersion());
            elector.setLeader(data.getSenderId());
            elector.setRole(NodeRole.FOLLOWER);
            logger.info("leader '{}' is elected, election version is '{}'", data.getSenderId(), elector.getVersionBySubject(Subject.ELECT_LEADER));
            response(data, Result.AGREE);
        });

        // 发起选举
        requestHandlers.put(Subject.ELECT_LEADER, (data) -> {
            AtomicLong localVersion = elector.getVersionBySubject(data.getSubject());
            if (data.getVersion() <= localVersion.get() || null != elector.getLeader()) {
                // 过期的议题、或者才被处理过的议题，直接拒绝, 已经有leader也拒绝
                response(data, Result.REJECT);
                logger.info("'{}' received ELECT_LEADER(local:{}, rec:{}) request from '{}', rejected!",
                        elector.getServer().getServerId(), localVersion.get(), data.getVersion(), data.getSenderId());
            } else {
                // 同意
                response(data, Result.AGREE);
                logger.info("'{}' received ELECT_LEADER(local:{}, rec:{}) request from '{}', agreed!",
                        elector.getServer().getServerId(), localVersion.get(), data.getVersion(), data.getSenderId());
                // 更新版本号
                updateLocalVersion(data, localVersion);
            }
        });

        // 申请加入网络
        requestHandlers.put(Subject.ASK_FOR_JOIN, (data) -> {
           if (elector.getRole() == NodeRole.LEADER) {
                logger.info("'{}' ask for joining us", data.getSenderId());
                elector.broadCastLeader();
           }
        });

    }

    @Override
    public void process(ProtocolData data) {
        DataHandler handler = requestHandlers.get(data.getSubject());
        if (null != handler) {
            handler.process(data);
        }
    }

    private void updateLocalVersion(ProtocolData data, AtomicLong localVersion) {
        localVersion.set(data.getVersion());
    }

    private void response(ProtocolData data, Result result) {
        ProtocolData protocolData = new ProtocolData(PacketType.RESPONSE, data.getSubject(), result, data.getVersion(), elector.getServer().getServerId());
        protocolData.setReceiver(data.getSenderId());
        protocolData.setPort(elector.getPort());
        try {
            protocolData.setHost(InetAddress.getLocalHost().getHostAddress());
            byte[] bytes = PacketSerializer.serialize(protocolData);
            DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getByName(elector.getMulticastHost()), elector.getPort());
            elector.getServer().send(dp);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
