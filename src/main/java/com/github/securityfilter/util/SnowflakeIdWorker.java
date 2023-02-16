package com.github.securityfilter.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SnowflakeIdWorker {

    // ==============================Fields===========================================
    /**
     * Start time cutoff (object creation time)
     */
    private final long twepoch = System.currentTimeMillis();

    /**
     * The number of bits of machine id
     */
    private final long workerIdBits = 5L;

    /**
     * The number of digits occupied by the data id
     */
    private final long datacenterIdBits = 5L;

    /**
     * The maximum machine id supported, which is 31 (this shift algorithm can quickly calculate the maximum number of decimal digits that can be represented by several binary digits)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * The maximum supported data id, which is 31
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * The number of bits of a sequence in an id
     */
    private final long sequenceBits = 12L;

    /**
     * The machine ID moves 12 bits to the left
     */
    private final long workerIdShift = sequenceBits;

    /**
     * Data id moves 17 bits to the left (12+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * So our time intercept is going to be 22 to the left.
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * Generate the mask for the sequence, which is 4095 (0b111111111111=0xfff=4095)
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * Working machine ID(0~31)
     */
    private final long workerId;

    /**
     * Data center ID(0~31)
     */
    private final long datacenterId;

    /**
     * Millisecond sequence(0~4095)
     */
    private long sequence = 0L;

    /**
     * The last time the ID was generated
     */
    private volatile long lastTimestamp = -1L;

    //==============================Constructors=====================================

    /**
     * The constructor
     */
    public SnowflakeIdWorker() {
        this(new Random().nextInt(31), new Random().nextInt(31));
    }

    /**
     * The constructor
     *
     * @param workerId     Work ID (0~31)
     * @param datacenterId Data center ID (0~31)
     */
    private SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ==============================Methods==========================================

    /**
     * Get the next ID
     *
     * @return SnowflakeId
     */
    public long nextId() {
        long timestamp = System.currentTimeMillis();

        //If the current time is less than the timestamp generated by the last ID, an exception should be thrown when the system clock has gone back
        if (timestamp < lastTimestamp) {
            timestamp = Math.abs(ThreadLocalRandom.current().nextLong(timestamp));
        }

        //If it is generated at the same time, the millisecond sequence is performed
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //Sequence overflow in milliseconds
            if (sequence == 0) {
                //Block to the next millisecond and get the new timestamp
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            //The timestamp changes and the sequence is reset in milliseconds
            sequence = 0L;
        }

        //The last time the ID was generated
        lastTimestamp = timestamp;

        //The shift and or operations are combined to form a 64-bit ID
        return ((Math.abs(timestamp - twepoch)) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * Block to the next millisecond until a new timestamp is obtained
     *
     * @param lastTimestamp The last time the ID was generated
     * @return Current timestamp
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}