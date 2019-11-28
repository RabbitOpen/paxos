package rabbit.algorithm.paxos.elect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.algorithm.paxos.elect.handler.RequestHandler;
import rabbit.algorithm.paxos.elect.handler.ResponseHandler;
import rabbit.algorithm.paxos.protocol.NodeRole;
import rabbit.algorithm.paxos.protocol.PacketType;
import rabbit.algorithm.paxos.protocol.ProtocolData;
import rabbit.algorithm.paxos.protocol.Subject;
import rabbit.algorithm.paxos.util.PacketSerializer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 选举器
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class LeaderElector {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private UdpServer server;

    // 选举线程
    private ElectionThread electThread;

    private ReentrantLock lock = new ReentrantLock();

    private Semaphore semaphore = new Semaphore(0);

    // 包处理器
    private Map<PacketType, DataHandler> handlerMap = new EnumMap<>(PacketType.class);

    // 收到的同意票数
    private AtomicLong tickets = new AtomicLong(0);

    // 总节点数
    private int nodeNum = 0;

    // leader节点
    private String leader;

    private NodeRole role = NodeRole.OBSERVER;

    // 议题版本本地缓存
    private Map<Subject, AtomicLong> versionMap = new EnumMap<>(Subject.class);

    private String multicastHost;

    private int port;

    public LeaderElector(String multicastHost, int port, int nodeNum) {
        initUdpServer(multicastHost, port);
        initPacketHandler();
        initSubjectsLocalVersion();
        initVoter();
        this.nodeNum = nodeNum;
        this.multicastHost = multicastHost;
        this.port = port;
    }

    private void initVoter() {
        electThread = new ElectionThread(this);
        electThread.start();
    }

    private void initUdpServer(String multicastHost, int port) {
        server = new UdpServer(dp -> {
            try {
                ProtocolData protocol = (ProtocolData) PacketSerializer.deserialize(dp.getData());
                lock.lock();
                if (server.getServerId().equals(protocol.getSenderId())) {
                    // 忽略自己发送的包
                    return;
                }
                handlerMap.get(protocol.getPackageType()).process(protocol);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }, port, multicastHost);
        server.start();
    }

    /**
     * 加入网络
     * @author xiaoqianbin
     * @date 2019/11/26
     **/
    public void joinNetwork() {
        if (!tryJoin()) {
            startVote();
        }
    }

    private void startVote() {
        logger.info("begin to elect a leader");
        tickets.set(1);
        electThread.beginElection();
    }


    private boolean tryJoin() {
        semaphore.drainPermits();
        ProtocolData askForJoin = new ProtocolData(PacketType.REQUEST, Subject.ASK_FOR_JOIN, 0, server.getServerId());
        sendProtocolData(askForJoin);
        try {
            logger.info("try to join a existed network");
            semaphore.tryAcquire(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (null != getLeader()) {
            logger.info("join success, leader is {}", getLeader());
            return true;
        } else {
            logger.info("join overtime(3s)!");
        }
        return false;
    }

    /**
     * 收到同意的响应
     * @author xiaoqianbin
     * @date 2019/11/26
     **/
    public void incrementAgreedTickets() {
        long current = this.tickets.addAndGet(1);
        if (current == ((nodeNum / 2) + 1)) {
            // 我被选中成leader了
            setRole(NodeRole.LEADER);
            setLeader(server.getServerId());
            endVote();
            // 广播leader节点
            logger.info("'{}' becomes leader!, election version is '{}'", server.getServerId(), getVersionBySubject(Subject.ELECT_LEADER));
            broadCastLeader();
        }
    }

    // 广播leader节点
    public void broadCastLeader() {
        try {
            lock.lock();
            // 广播leader节点时使用选举leader的版本信息
            ProtocolData electLeaderData = new ProtocolData(PacketType.REQUEST, Subject.BROADCAST_LEADER,
                    versionMap.get(Subject.ELECT_LEADER).get(), server.getServerId());
            sendProtocolData(electLeaderData);
        } finally {
            lock.unlock();
        }
    }

    public void broadCastElectionPacket() {
        try {
            lock.lock();
            if (null != getLeader()) {
                logger.warn("leader[{}] has been elected, election is over!", getLeader());
                return;
            }
            versionMap.get(Subject.ELECT_LEADER).addAndGet(1);
            ProtocolData electLeaderData = new ProtocolData(PacketType.REQUEST, Subject.ELECT_LEADER,
                    versionMap.get(Subject.ELECT_LEADER).get(), server.getServerId());
            logger.info("'{}' broadcast a election package, version is {}", server.getServerId(), versionMap.get(Subject.ELECT_LEADER).get());
            sendProtocolData(electLeaderData);
        } finally {
            lock.unlock();
        }
    }

    private void sendProtocolData(ProtocolData electLeaderData) {
        try {
            byte[] bytes = PacketSerializer.serialize(electLeaderData);
            DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getByName(multicastHost), port);
            server.send(dp);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public NodeRole getRole() {
        return role;
    }

    public void endVote() {
        semaphore.release();
        electThread.endElection();
    }

    public void close() {
        electThread.close();
        server.close();
    }

    private void initPacketHandler() {
        handlerMap.put(PacketType.REQUEST, new RequestHandler(this));
        handlerMap.put(PacketType.RESPONSE, new ResponseHandler(this));
    }

    // 初始化本地议题版本缓存
    private void initSubjectsLocalVersion() {
        for (Subject value : Subject.values()) {
            versionMap.put(value, new AtomicLong(0));
        }
    }

    public AtomicLong getVersionBySubject(Subject subject) {
        return versionMap.get(subject);
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getLeader() {
        return leader;
    }

    public UdpServer getServer() {
        return server;
    }

    public String getMulticastHost() {
        return multicastHost;
    }

    public int getPort() {
        return port;
    }
}
