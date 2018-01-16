package com.tuling.netty.snake_game;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 蛇蛇游戏引擎
 * Created by Tommy on 2018/1/9.
 */
public class SnakeGameEngine {
    static final Logger logger = LoggerFactory.getLogger(SnakeGameEngine.class);
    public Map<String, SnakeEntity> snakeNodes = new HashMap<>();
    private final int mapWidth;
    private final int mapHeight;

    // 存储了地图上所有的节点
    private final Mark mapsMarks[];
    // 刷新间隔(毫秒)
    private final int refreshTime;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> future;
    private SnakeGameListener listener;
    private Long currentVersion = 0L;
    private volatile LinkedList<VersionData> historyVersionData = new LinkedList();
    private volatile VersionData currentMapData = null;
    private static final int historyVersionMax = 20;
    private ArrayList<Food> foods = new ArrayList<>();
    private int footMaxSize = 10;

    public SnakeGameEngine() {
        mapWidth = 400;
        mapHeight = 300;
        refreshTime = 200;
        mapsMarks = new Mark[mapWidth * mapHeight];
    }

    public SnakeGameEngine(int mapWidth, int mapHeight, int refreshTime) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.refreshTime = refreshTime;
        mapsMarks = new Mark[mapWidth * mapHeight];
    }


    // 启动
    public void start() {
        logger.info("游戏引擎启动...");
        // 固定间隔执行任务
        future = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                gameTimeStep();
            }
        }, refreshTime, refreshTime, TimeUnit.MILLISECONDS);
    }

    //animate
    public void gameTimeStep() {
        try {
            build();
        } catch (Throwable e) {
            logger.error("地图构建异常", e);
        } finally {
            afterBuild();
        }
    }

    private void build() {
        /**
         * 基于状态执行算法
         */
        for (SnakeEntity snake : snakeNodes.values()) {
            switch (snake.getState()) {
                case inactive:
                    snake.active();
                    break;
                case alive:
                    snake.moveStep();
                    break;
                case grow:
                    snake.addToHead();
                    snake.moveStep();
                    break;
                case dying:
                    snake.die();
                    break;
                case die:
                    break;
            }
        }

        // 当前版本 新增的节点
        ArrayList<Integer[]> changeNodes = new ArrayList<>();


        /**
         * 执行触发的游戏规则
         */
        for (SnakeEntity snake : snakeNodes.values()) {
            for (Integer[] node : snake.getAddNodes()) {
                //断定是否撞击蛇身
                if (getMark(node).snakeNodes > 1) {
                    snake.dying();
                } else if (node[0] <= 0 || node[0] >= mapHeight - 1 // 撞击边界
                        || node[1] <= 0 || node[1] >= mapWidth - 1) {
                    snake.dying();
                } else if (getMark(node).footNode > 0) { //吃掉食物
                    digestionFood(snake, node); // 消化食物
//                  changeNodes.add(node);
                }
            }

            changeNodes.addAll(snake.getAddNodes());
            changeNodes.addAll(snake.getRemoveNodes());
        }


        // 投放规定量食物
        while (foods.size() < footMaxSize) {
            Food food = grantFood();
            changeNodes.add(food.point);
        }


        // 如果变更不为空，则创建新的版本号
        if (!changeNodes.isEmpty()) {
            /**
             * 编码新版本数据
             */
            long newVersion = currentVersion + 1;


            /**
             * 版本归档存储
             */
            while (historyVersionData.size() >= historyVersionMax) {
                historyVersionData.removeLast();
            }
            VersionData changeData = encodeVersion(newVersion, changeNodes);
            historyVersionData.addFirst(changeData);

            /**
             * 变更版本号
             */
            currentVersion = newVersion;
            // 刷新整体地图
            getCurrentMapData(true);
            /**
             * 通知版本变更
             */
            if (listener != null) {
                try {
                    listener.versionChange(changeData, null);
                } catch (Exception e) {
                    logger.error("版本变更通知失败", e);
                }
            }

        }
    }

    private void afterBuild() {
        for (SnakeEntity snake : snakeNodes.values()) {
            snake.flush();
        }
    }

    public Food grantFood() {
        // 随机生成的投放点
        int releasePoint = -1;

        Random random = new Random();
        int start = random.nextInt(mapHeight * mapHeight - 5) + 4;
        int nextCount = random.nextInt(50);

        // i 查找开始位置
        // n 从开始位置起的第 n个空位,如果所有空位没有n个，则往后减
        // m 遍历的最大值,防止无限循环
        for (int i = start, n = 0, m = 0; i < mapsMarks.length
                && m < mapsMarks.length; i++, m++) {
            if (mapsMarks[i] == null || mapsMarks[i].isEmpty()) {
                n++;
                releasePoint = i;
                if (n >= nextCount) {
                    break;
                }
            }
            // 重新从0开始遍历
            if (i == mapsMarks.length - 1) {
                i = 0;
            }
        }
        if (releasePoint > -1) {
            Integer[] point = new Integer[]{releasePoint / mapWidth, releasePoint % mapWidth};
            Food food = new Food(point, 1);
            foods.add(food);
            getMark(point).footNode = 1;
            return food;
        } else {
            throw new RuntimeException("投食失败。无法找到空位投食");
        }
    }

    // 吃掉食物
    private Food digestionFood(SnakeEntity snake, Integer[] point) {
        Food food = null;
        for (Food f : foods) {
            if (Arrays.equals(f.point, point)) {
                food = f;
                break;
            }
        }
        if (food == null) {
            throw new RuntimeException(
                    String.format("消化食物异常，坐标上不存在指定食物x:%s,y:%s", point[1], point[00]));
        }

        foods.remove(food); // 从食物列表中移除
        getMark(point).footNode = 0;// 清除地图中食物标记状态
        snake.grow();// 指定角色为增长状态
        logger.info("吃掉食物 位置信息：x={},y={},角色信息:{}", point[1], point[0], snake.toString());
        return food;
    }


    // 停止运行中的地图
    public void stop() {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    public SnakeEntity newSnake(String accountId, String accountName) {
        int max = Math.min(mapWidth, mapHeight) - 30;
        int min = 30;
        Random random = new Random();
        // 随机生成 出生点位
        int startPoint = random.nextInt(max - min + 1) + min;
        SnakeEntity node = new SnakeEntity(this, accountId, startPoint,
                3, SnakeEntity.Direction.right);
        node.setGameName(accountName);
        snakeNodes.put(node.getAccountId(), node);
        this.logger.info("新增Snake ID:{} 出生点位:{} 初始节点:{}", accountId, startPoint, 3);
        return node;
    }

    public void controlSnake(String accountId, int controlCode) {
        if (!snakeNodes.containsKey(accountId)) {
            logger.warn("找不到指定帐户");
            return;
        }

        SnakeEntity snake = snakeNodes.get(accountId);
        switch (controlCode) {
            case 37:
                snake.setDirection(SnakeEntity.Direction.left);
                break;
            case 38:
                snake.setDirection(SnakeEntity.Direction.up);
                break;
            case 39:
                snake.setDirection(SnakeEntity.Direction.right);
                break;
            case 40:
                snake.setDirection(SnakeEntity.Direction.down);
                break;
        }
        logger.debug("指令控制 ID:{},指令:{}", accountId, controlCode);
    }

    public Mark getMark(Integer[] point) {
        int index = point[0] * mapWidth + point[1];
        return getMark(index);
    }

    private Mark getMark(int index) {
        if (mapsMarks[index] == null) {
            mapsMarks[index] = new Mark();
        }
        return mapsMarks[index];
    }

    // 获取当前所有位点
    public ArrayList<Integer[]> getAllPoint() {
        ArrayList<Integer[]> allPoints = new ArrayList<>(2000);
        Integer x, y;
        for (int i = 0; i < mapsMarks.length; i++) {
            if (mapsMarks[i] != null && !mapsMarks[i].isEmpty()) {
                x = i % mapWidth;
                y = i / mapWidth;
                allPoints.add(new Integer[]{y, x});
            }
        }
        return allPoints;
    }

    // 构建当前地图所有的像素
    private VersionData encodeCurrentMapData() {
        StringBuilder body = new StringBuilder();
        StringBuilder food = new StringBuilder();
        Mark mark;
        int x, y;
        for (int i = 0; i < mapsMarks.length; i++) {
            mark = mapsMarks[i];
            x = i % mapWidth;
            y = i / mapWidth;
            if (mark != null && mark.snakeNodes > 0) {
                body.append("," + x + "," + y);
            } else if (mark != null && mark.footNode > 0) {
                food.append("," + x + "," + y);
            }
        }
        List<String> cmds = new ArrayList();
        List<String> cmdDatas = new ArrayList();

        // 去掉前缀 中的逗号
        if (body.length() > 0) {
            body.deleteCharAt(0);
            cmds.add("Lime");
            cmdDatas.add(body.toString());
        }
        if (food.length() > 0) {
            food.deleteCharAt(0);
            cmds.add("Yellow");
            cmdDatas.add(food.toString());
        }
        VersionData vd = new VersionData(currentVersion, System.currentTimeMillis());
        vd.setCmds(cmds.toArray(new String[cmds.size()]));
        vd.setCmdDatas(cmdDatas.toArray(new String[cmdDatas.size()]));
        vd.setFull(true);
        return vd;
    }

    // 构建当前版本地图像素的变更
    private VersionData encodeVersion(long version, ArrayList<Integer[]> changePoints) {
        StringBuilder body = new StringBuilder();
        StringBuilder food = new StringBuilder();
        StringBuilder remove = new StringBuilder();
        Mark mark;
        for (Integer[] p : changePoints) {
            mark = getMark(p);
            if (mark == null || mark.isEmpty()) {
                remove.append("," + p[1] + "," + p[0]);
            } else if (mark.snakeNodes > 0) {
                body.append("," + p[1] + "," + p[0]);
            } else if (mark.footNode > 0) {
                food.append("," + p[1] + "," + p[0]);
            }
        }
        List<String> cmds = new ArrayList();
        List<String> cmdDatas = new ArrayList();
        // 去掉前缀 中的逗号
        if (body.length() > 0) {
            body.deleteCharAt(0);
            cmds.add("Lime");
            cmdDatas.add(body.toString());
        }
        if (food.length() > 0) {
            food.deleteCharAt(0);
            cmds.add("Yellow");
            cmdDatas.add(food.toString());
        }
        if (remove.length() > 0) {
            remove.deleteCharAt(0);
            cmds.add("Black");
            cmdDatas.add(remove.toString());
        }

        VersionData vd = new VersionData(currentVersion, System.currentTimeMillis());
        vd.setCmds(cmds.toArray(new String[cmds.size()]));
        vd.setCmdDatas(cmdDatas.toArray(new String[cmdDatas.size()]));
        vd.setFull(false);
        return vd;
    }




    public VersionData getCurrentMapData(boolean check) {
        if (currentMapData == null) {
            currentMapData = encodeCurrentMapData();
        } else if (check && currentMapData.getVersion() < currentVersion) {
            currentMapData = encodeCurrentMapData();
        }
        return currentMapData;
    }

    public List<VersionData> getVersion(Long[] versionId) {
        List<VersionData> list = new ArrayList<>();
        for (VersionData historyVersion : historyVersionData) {
            for (long v : versionId) {
                if (historyVersion.getVersion() == v) {
                    list.add(historyVersion);
                }
            }
        }
        return list;
    }

    public void setListener(SnakeGameListener listener) {
        this.listener = listener;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }

    // 地图标记位
    static class Mark {
        public int snakeNodes = 0;
        public int footNode = 0;//1,2,3,4,5

        private boolean isEmpty() {
            return snakeNodes <= 0 && footNode <= 0;
        }
    }


    private static class Food {
        // 当前位位置
        private Integer[] point;
        private int type;

        public Food(Integer[] point, int type) {
            this.point = point;
            this.type = type;
        }
    }

    public static interface SnakeGameListener {
        public void versionChange(VersionData changeData, VersionData currentData);
    }


}
