public class SnowFlake {
    /*
    时间戳占用位数
     */
    private final int TIMESTAMP_BIT;
    /*
    机器标识占用位数
     */
    private final int MACHINE_BIT;
    /*
    最多可以表示机器的数量
     */
    private final long MAX_MACHINE_NUM;
    /*
    当前机器的机器标识
     */
    private final long MACHINE_NUM;
    /*
    序列号占用位数
     */
    private final int SEQUENCE_BIT;
    /*
    在同一机器的同一时间最多可以生成唯一ID的数量
     */
    private final long MAX_SEQUENCE_NUM;
    /*
    上一次时间戳
     */
    private long lastTimestamp;
    /*
    用于自增的序列号，默认为-1是防止第一次使用雪花算法时可能无法获取第0序号的id
    */
    private long RE_SEQUENCE = -1L;

    /**
     * @param MACHINE_NUM      当前机器标识
     * @param MAX_MACHINE_NUM  最大机器数量
     * @param MAX_SEQUENCE_NUM 最大序列数量
     */
    public SnowFlake(long MACHINE_NUM, long MAX_MACHINE_NUM, long MAX_SEQUENCE_NUM) {
        if (MAX_MACHINE_NUM < 2 || MAX_SEQUENCE_NUM < 2 || MACHINE_NUM < 0 || MACHINE_NUM >= MAX_MACHINE_NUM)
        /*
            如果机器标识数量为1，那么在分布式场景中，多台机器无法唯一确定；
            如果序列号为1，那么并发场景下同一台机器多次请求获取时间戳可能存在相同的情况造成id相同；
            如果机器标识或序列号<=0,不符合实际意义；
            综上所述：如果机器标识或序列号数量<=2,那么没必要使用雪花算法，雪花算法失去了原本的意义。
         */
            throw new RuntimeException("机器标识或序列号必须>2！");
        MACHINE_BIT = 64 - Long.numberOfLeadingZeros(MAX_MACHINE_NUM - 1);
        SEQUENCE_BIT = 64 - Long.numberOfLeadingZeros(MAX_SEQUENCE_NUM - 1);
        //时间戳位数=64-机器标识位数-序列号位数-最高位符号位1位
        TIMESTAMP_BIT = 64 - MACHINE_BIT - SEQUENCE_BIT - 1;
        if (TIMESTAMP_BIT < 64 - Long.numberOfLeadingZeros(System.currentTimeMillis()))
            throw new RuntimeException("存储机器标识数量＋序列号数量的位数过大，导致时间戳位数不够！");
        this.MAX_MACHINE_NUM = MAX_MACHINE_NUM;
        this.MAX_SEQUENCE_NUM = MAX_SEQUENCE_NUM;
        this.MACHINE_NUM = MACHINE_NUM;
        lastTimestamp = System.currentTimeMillis();
    }

    /**
     * 获取全局唯一的id；
     * 如果id中用于存储时间戳的位数无法存储当前时间戳，抛出异常；
     * 如果当前时间戳比上一次获取的时间戳还小，说明时间被往前调了，抛出异常；
     * 这是一个安全的方法；
     *
     * @return 返回long型id值
     */
    public synchronized long nextId() {
        //获取当前时间戳
        long nowTimeStamp = System.currentTimeMillis();
        //用于检查当前时间戳位数是否正确，在一定年限内，此步骤可省略
        CheckTimestamp(nowTimeStamp);
        //当前时间戳比上一次获取的时间戳还小，说明时间被往前调了，抛出异常
        if (nowTimeStamp < lastTimestamp)
            throw new RuntimeException("时钟倒退，拒绝生成id！");
        //拿到了重复的时间戳
        if (nowTimeStamp == lastTimestamp) {
            //序列号自增
            RE_SEQUENCE++;
            //序列号已使用完，则复位序列号并调用nextMillis()方法获取到下一个不同的时间戳
            if (RE_SEQUENCE == MAX_SEQUENCE_NUM) {
                RE_SEQUENCE = 0L;
                nowTimeStamp = nextMillis();
            }
        } else
            //序列号复位
            RE_SEQUENCE = 0L;
        //置上一个时间戳为当前时间戳
        lastTimestamp = nowTimeStamp;
        //返回生成的id
        return nowTimeStamp << (Long.numberOfLeadingZeros(nowTimeStamp) - 1)
                | MACHINE_NUM << SEQUENCE_BIT
                | RE_SEQUENCE;
    }

    /**
     * 获取与上一次获取的时间戳不同的时间戳
     *
     * @return 新的时间戳
     */
    private long nextMillis() {
        long nowTimeStamp = System.currentTimeMillis();
        //通过自旋完成
        while (nowTimeStamp <= lastTimestamp)
            nowTimeStamp = System.currentTimeMillis();
        return nowTimeStamp;
    }

    /**
     * 检查timeStamp的位数是否越界
     *
     * @param timeStamp 待检查的时间戳
     */
    private void CheckTimestamp(long timeStamp) {
        if (TIMESTAMP_BIT < 64 - Long.numberOfLeadingZeros(timeStamp))
            throw new RuntimeException("时间戳位数不够!");
    }

    public long getMAX_MACHINE_NUM() {
        return MAX_MACHINE_NUM;
    }

    public long getMACHINE_NUM() {
        return MACHINE_NUM;
    }

    public long getMAX_SEQUENCE_NUM() {
        return MAX_SEQUENCE_NUM;
    }
}