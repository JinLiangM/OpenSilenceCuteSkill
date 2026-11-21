package lol.silencecute.client.anim;

public final class UiAnimation {

    private UiAnimation() {}

    public enum Direction { FORWARDS, BACKWARDS }

    public static class TimerUtil {
        private long lastMS = System.currentTimeMillis();

        public long getTime() {
            return System.currentTimeMillis() - lastMS;
        }

        public void setTime(long time) {
            lastMS = System.currentTimeMillis() - time;
        }
    }

    public static abstract class Animation {
        public TimerUtil timerUtil = new TimerUtil();
        protected int duration;
        protected double endPoint;
        protected Direction direction;

        public Animation(int ms, double endPoint) {
            this(ms, endPoint, Direction.FORWARDS);
        }

        public Animation(int ms, double endPoint, Direction direction) {
            this.duration = ms;
            this.endPoint = endPoint;
            this.direction = direction;
        }

        public void setDirection(Direction direction) {
            if (this.direction != direction) {
                this.direction = direction;
                this.timerUtil.setTime((long) this.duration
                        - Math.min(this.duration, this.timerUtil.getTime()));
            }
        }

        public boolean isDone() {
            return timerUtil.getTime() >= duration;
        }

        protected abstract double getEquation(double x);

        public double getOutput() {
            if (direction == Direction.FORWARDS) {
                if (isDone()) return endPoint;
                return getEquation((double) timerUtil.getTime() / duration) * endPoint;
            } else {
                if (isDone()) return 0.0;
                return (1.0 - getEquation((double) timerUtil.getTime() / duration)) * endPoint;
            }
        }
    }

    public static class DecelerateAnimation extends Animation {
        public DecelerateAnimation(int ms, double endPoint) {
            super(ms, endPoint);
        }

        public DecelerateAnimation(int ms, double endPoint, Direction direction) {
            super(ms, endPoint, direction);
        }

        @Override
        protected double getEquation(double x) {
            return 1.0 - (x - 1.0) * (x - 1.0);
        }
    }
}
