package rabbit.algorithm.paxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.algorithm.paxos.elect.LeaderElector;

/**
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class PaxosMainEntry {

    private static Logger logger = LoggerFactory.getLogger(PaxosMainEntry.class);

    public static void main(String[] args) {
        logger.info("start ");
        new Thread(() -> {
            LeaderElector elector = new LeaderElector("227.1.1.1", 9527, 3);
            elector.joinNetwork();
        }).start();
    }

}
