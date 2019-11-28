package rabbit.algorithm.paxos.elect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 投票线程
 * @author xiaoqianbin
 * @date 2019/11/26
 **/
public class ElectionThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private boolean closed = false;

    private Semaphore activeSemaphore = new Semaphore(0);

    private Semaphore voteSemaphore = new Semaphore(0);

    private LeaderElector elector;

    // 选票是否已激活
    private boolean active = false;

    public ElectionThread(LeaderElector elector) {
        this.elector = elector;
        this.setName("voter-thread-" + getId());
    }

    @Override
    public void run() {
        while (!closed) {
            try {
                if (!active) {
                    waitUntilActive();
                }
                if (closed) {
                    break;
                }
                elector.broadCastElectionPacket();
                if (voteSemaphore.tryAcquire(3000 + new Random().nextInt(1000), TimeUnit.MILLISECONDS)) {
                    logger.info("election is over");
                    active = false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void endElection() {
        voteSemaphore.release();
    }

    private void waitUntilActive() throws InterruptedException {
        activeSemaphore.acquire();
    }

    public void close() {
        closed = true;
        activeSemaphore.release();
        try {
            join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("voter thread closed");
    }

    // 发起投票
    public void beginElection() {
        active = true;
        activeSemaphore.release();
    }
}
