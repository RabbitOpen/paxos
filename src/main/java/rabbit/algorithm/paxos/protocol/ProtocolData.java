package rabbit.algorithm.paxos.protocol;

import java.io.Serializable;

/**
 * 协议包
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class ProtocolData implements Serializable {

    // 包类型
    private PacketType packageType;

    // 议题
    private Subject subject;

    // 投票结果
    private Result result;

    // 版本号
    private long version;

    // 发送者
    private String senderId;

    // 接收者
    private String receiver;

    private String host;

    private int port;

    public ProtocolData(PacketType packageType, Subject subject, long version, String senderId) {
        this.packageType = packageType;
        this.subject = subject;
        this.version = version;
        this.senderId = senderId;
    }

    public ProtocolData(PacketType packageType, Subject subject, Result result, long version, String senderId) {
        this.packageType = packageType;
        this.subject = subject;
        this.result = result;
        this.version = version;
        this.senderId = senderId;
    }

    public PacketType getPackageType() {
        return packageType;
    }

    public void setPackageType(PacketType packageType) {
        this.packageType = packageType;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Result getResult() {
        return result;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
