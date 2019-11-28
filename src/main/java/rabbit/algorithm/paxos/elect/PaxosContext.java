package rabbit.algorithm.paxos.elect;

import rabbit.algorithm.paxos.protocol.Subject;

import java.util.EnumMap;
import java.util.Map;

/**
 * 环境信息上下文
 * @author xiaoqianbin
 * @date 2019/11/26
 **/
public class PaxosContext {

    // 议题信息
    private static Map<Subject, Long> subjectContext = new EnumMap<>(Subject.class);

    // 议题冷却时间
    private static final int coolDownTime = 10_000;

    /**
     * 标记当前议题
     * @param	subject
     * @author  xiaoqianbin
     * @date    2019/11/26
     **/
    public static void hotSubject(Subject subject) {
        subjectContext.put(subject, System.currentTimeMillis());
    }

    /**
     * 判断当前议题是否最近才被处理过
     * @param	subject
     * @author  xiaoqianbin
     * @date    2019/11/26
     **/
    public static boolean isHotSubject(Subject subject) {
        return subjectContext.containsKey(subject) &&
                (System.currentTimeMillis() - subjectContext.get(subject) < coolDownTime);
    }

}
