package com.zdragon.videoio;

/**
 * Created by 90678 on 2017/7/30.
 */

public class Constants {

    public static int PHONE_WIDTH = (int) 480;

    public static int PHONE_HEIGHT = (int) 640;

    public static final int CAR_PAD_WIDTH = PHONE_WIDTH;

    public static final int CAR_PAD_HEIGHT = PHONE_HEIGHT;

    //    public static final int BITRATE = 1024000 * 10;
    public static final int BITRATE = (int) (PHONE_WIDTH * PHONE_HEIGHT);

    public static final int FPS = 60;

    public static final int KEY_I_FRAME_INTERVAL = 5;

    public static final int DPI = PHONE_HEIGHT / PHONE_WIDTH;

    public static final int PORT = 9005;

    public static final int PORT_CONTROL = 9006;

    public static final int DECODE_TIMEOUT_USEC = 1000 * 1000;

    public static final int ENCODE_TIMEOUT_USEC = DECODE_TIMEOUT_USEC;

}
