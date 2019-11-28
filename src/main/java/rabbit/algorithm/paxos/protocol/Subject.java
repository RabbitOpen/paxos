package rabbit.algorithm.paxos.protocol;

/**
 * 议题
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public enum Subject {

    BROADCAST_LEADER("领导选举出来后发的通知"),

    ASK_FOR_JOIN("申请加入网络"),

    ELECT_LEADER("选举Leader");

    Subject(String desc) {

    }

}
