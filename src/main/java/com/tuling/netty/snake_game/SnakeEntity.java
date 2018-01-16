package com.tuling.netty.snake_game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by Tommy on 2018/1/8.
 */
public class SnakeEntity {
    static final Logger logger = LoggerFactory.getLogger(SnakeEntity.class);

    private final int startPoint;
    private final int initBodySzie;
    private final SnakeGameEngine engine;
    private String accountId;
    private String gameName;
    private Direction direction;
    private ArrayList<Integer[]> bodys = new ArrayList<>();//y,x
    public ArrayList<Integer[]> addNodes = new ArrayList<>();
    public ArrayList<Integer[]> removeNodes = new ArrayList<>();

    public State state;

    public enum State {
        /**
         * 待激活
         */
        inactive,
        /**
         * 正常存活
         */
        alive,
        /**
         * 生长
         */
        grow,
        /**
         *待死亡
         */
        dying,
        /**
         *死亡
         */
        die
    }

    public enum Direction {
        up, down, left, right;
    }

    public SnakeEntity(SnakeGameEngine engine, String accountId, int startPoint, int initBodySzie, Direction defaultDirection) {
        this.engine = engine;
        this.accountId = accountId;
        this.direction = defaultDirection;
        state = State.inactive;
        this.startPoint = startPoint;
        this.initBodySzie = initBodySzie;

    }

    public void setDirection(Direction direction) {
        // 无效指令验证 .
        if (this.direction == Direction.up && direction == Direction.down)
            return;
        if (this.direction == Direction.down && direction == Direction.up)
            return;
        if (this.direction == Direction.left && direction == Direction.right)
            return;
        if (this.direction == Direction.right && direction == Direction.left)
            return;

        logger.debug("改变snake 移动方向 ID:{},当前方向:{}", accountId, direction.toString());
        this.direction = direction;
    }

    // 上前前动一步
    public void moveStep() {

        addToHead();
        removeToTail();
    }

    public void removeToTail() {
        Integer[] node = bodys.remove(bodys.size() - 1);
        removeNodes.add(node);
        engine.getMark(node).snakeNodes--;
    }

    //0,0 0,1 0,2 0,3 0,4 0,5
    //1,0 1,1 1,2 1,3 1,4 1,5
    //2,0 2,1 2,2 2,3 2,4 2,5
    //3,0 3,1 3,2 3,3 3,4 3,5
    // 向前添加一个节点
    public void addToHead() {
        Integer[] first = bodys.get(0);
        Integer[] newFirst;
        switch (direction) {
            case up:
                newFirst = new Integer[]{first[0] - 1, first[1]};
                break;
            case down:
                newFirst = new Integer[]{first[0] + 1, first[1]};
                break;
            case left:
                newFirst = new Integer[]{first[0], first[1] - 1};
                break;
            case right:
                newFirst = new Integer[]{first[0], first[1] + 1};
                break;
            default:
                throw new RuntimeException("direction must not null");
        }

       /* bodys.add(0, newFirst);
        addNodes.add(newFirst);
        engine.getMark(newFirst).snakeNodes++;*/

        add(0, newFirst);
    }

    private void add(int index, Integer[] point) {
        bodys.add(index, point);
        addNodes.add(point);
        engine.getMark(point).snakeNodes++;
        logger.debug("添加节点 x:{} y:{}",point[1],point[0]);
    }

    public String getAccountId() {
        return accountId;
    }

    public Direction getDirection() {
        return direction;
    }

    public ArrayList<Integer[]> getBodys() {
        return bodys;
    }

    public ArrayList<Integer[]> getAddNodes() {
        return addNodes;
    }

    public ArrayList<Integer[]> getRemoveNodes() {
        return removeNodes;
    }


    public State getState() {
        return state;
    }

    // 生长一节
    public void grow(){
        this.state = State.grow;
        logger.info(" id:{} name:{}", accountId, gameName);
    }
    public void die() {
        int bodySize = bodys.size();
        for (int i = 0; i < bodySize; i++) {
            removeToTail();
        }
        this.state = State.die;
        logger.info("清除死亡角色 id:{} name:{}", accountId, gameName);
    }

    public void dying() {
        this.state = State.dying;
        logger.info("角色死亡 id:{} name:{}", accountId, gameName);
    }


    public boolean isDie() {
        return this.state == State.die;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public void flush() {
        addNodes.clear();
        removeNodes.clear();
    }

    public void active() {
        Integer[] first = new Integer[]{startPoint, startPoint};
        // 添加初始节点
        add(0, first);

        // 添加蛇身
        for (int i = 1; i < initBodySzie; i++) {
            addToHead();
        }
        this.state = State.alive;
    }

    @Override
    public String toString() {
        return "accountId='" + accountId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", state=" + state   +
                ",direction="+direction;
    }
}
