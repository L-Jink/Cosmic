package server.liveops;

/** The per-world rates the calendar reads and changes. Backed by {@code net.server.world.World} in production. */
public interface RateSink {
    int getExpRate();
    void setExpRate(int rate);
    int getDropRate();
    void setDropRate(int rate);
    int getMesoRate();
    void setMesoRate(int rate);
}
